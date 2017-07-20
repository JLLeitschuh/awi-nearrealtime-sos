package org.n52.sensorweb.awi.data;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.hibernate.SessionFactory;
import org.joda.time.DateTime;

import org.n52.janmayen.IntervalMap;
import org.n52.janmayen.IntervalTree;
import org.n52.janmayen.function.Functions;
import org.n52.janmayen.lifecycle.Constructable;
import org.n52.sensorweb.awi.data.entities.Expedition;
import org.n52.sensorweb.awi.util.DelegatingTimerTask;
import org.n52.sos.ds.hibernate.util.AbstractSessionDao;

/**
 * {@code FeatureCache} implemnetation that reads all feature identifiers from the database and keeps the in an
 * {@link IntervalMap}.
 *
 * @author Christian Autermann
 */
public class FeatureCacheImpl extends AbstractSessionDao implements Constructable, FeatureCache {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final long updateInterval;
    private final Timer timer = new Timer(true);
    private Map<String, Set<String>> byPlatform;
    private Map<String, IntervalMap<DateTime, String>> byTime;

    /**
     * Creates a new {@code FeatureCache}.
     *
     * @param sessionFactory the session factory
     * @param updateInterval the interval to update the cache
     */
    public FeatureCacheImpl(SessionFactory sessionFactory, long updateInterval) {
        super(sessionFactory);
        this.updateInterval = updateInterval;
    }

    @Override
    public Set<String> getFeatureIds(String platform) {
        Objects.requireNonNull(platform);
        this.lock.readLock().lock();
        try {
            return getByPlatform(platform).collect(toSet());
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public String getFeatureId(String platform, DateTime time) {
        Objects.requireNonNull(platform);
        Objects.requireNonNull(time);
        this.lock.readLock().lock();
        try {
            return this.byTime.getOrDefault(platform, IntervalMap.universal(platform))
                    .getOrDefault(time, platform);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    /**
     * Get a stream of feature identifiers for the specified platform.
     *
     * @param platform the platform
     *
     * @return the feature identifiers
     */
    private Stream<String> getByPlatform(String platform) {
        this.lock.readLock().lock();
        try {
            return this.byPlatform.getOrDefault(platform, Collections.singleton(platform)).stream();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void init() {
        if (this.updateInterval > 0) {
            updateAndSchedule();
        } else {
            update();
        }
    }

    /**
     * Update the cache and schedule a new update.
     */
    private void updateAndSchedule() {
        try {
            update();
        } finally {
            this.timer.schedule(new DelegatingTimerTask(this::updateAndSchedule), this.updateInterval);
        }
    }

    /**
     * Update the cache.
     */
    private void update() {
        this.lock.writeLock().lock();
        try {
            @SuppressWarnings("unchecked")
            List<Expedition> expeditions = query(s -> s.createCriteria(Expedition.class)
                    .setComment("Caching expedition time intervals").list());
            this.byTime = expeditions.stream()
                    .collect(groupingBy(Expedition::getPlatform, Collector.of(HashSet::new, Set::add,
                                                                              Functions.mergeLeft(Set::addAll),
                                                                              this::createIntervalMap)));
            this.byPlatform = expeditions.stream()
                    .collect(groupingBy(Expedition::getPlatform, mapping(Expedition::getName, toSet())));
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * Create a {@link IntervalMap} for the expeditions.
     *
     * @param t the expeditions
     *
     * @return the interval map
     */
    private IntervalMap<DateTime, String> createIntervalMap(Set<Expedition> t) {
        IntervalTree<DateTime, String> tree = new IntervalTree<>();
        t.stream().filter(Expedition::isValid)
                .forEach(feature -> tree.add(new DateTime(feature.getBegin()),
                                             new DateTime(feature.getEnd()),
                                             feature.getName()));
        return tree;
    }

}
