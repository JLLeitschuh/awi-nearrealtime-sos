package org.n52.sensorweb.awi.geometry;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.joda.time.DateTime;

import org.n52.janmayen.function.Functions;
import org.n52.janmayen.lifecycle.Constructable;
import org.n52.sensorweb.awi.data.entities.Expedition;
import org.n52.sensorweb.awi.util.DelegatingTimerTask;
import org.n52.sensorweb.awi.util.IntervalTree;


/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class ExpeditionDaoImpl implements Constructable, ExpeditionDao {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final long updateInterval;
    private final Timer timer = new Timer(true);
    private Map<String, Set<String>> byPlatform;
    private Map<String, IntervalTree<DateTime, String>> byTime;
    private final SessionFactory sessionFactory;

    public ExpeditionDaoImpl(SessionFactory sessionFactory, long updateInterval) {
        this.sessionFactory = sessionFactory;
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
    public Optional<String> getFeatureId(String platform, DateTime time) {
        Objects.requireNonNull(platform);
        Objects.requireNonNull(time);
        this.lock.readLock().lock();
        try {
            return this.byTime.get(platform).get(time);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private Stream<String> getByPlatform(String platform) {
        this.lock.readLock().lock();
        try {
            return this.byPlatform.getOrDefault(platform, Collections.emptySet()).stream();
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

    private void updateAndSchedule() {
        try {
            update();
        } finally {
            this.timer.schedule(new DelegatingTimerTask(this::updateAndSchedule), this.updateInterval);
        }
    }

    @SuppressWarnings("unchecked")
    private void update() {
        List<Expedition> list;

        Session session = sessionFactory.openSession();
        try {
            list = session.createCriteria(Expedition.class).list();
        } finally {
            session.close();
        }
        this.lock.writeLock().lock();
        try {
            Function<Expedition, String> key = e -> e.getPlatform().getCode();
            this.byTime = list.stream().collect(groupingBy(key, Collector.of(HashSet::new, Set::add,
                                                                             Functions.mergeLeft(Set::addAll),
                                                                             this::createIntervalTree)));
            this.byPlatform = list.stream().collect(groupingBy(key, mapping(Expedition::getName, toSet())));
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private IntervalTree<DateTime, String> createIntervalTree(Set<Expedition> t) {
        IntervalTree<DateTime, String> tree = new IntervalTree<>();
        t.stream()
                .filter(x -> x.getBegin().compareTo(x.getEnd()) <= 0)
                .forEach(feature -> tree.add(new DateTime(feature.getBegin()),
                                             new DateTime(feature.getEnd()),
                                             feature.getName()));
        return tree;
    }

}
