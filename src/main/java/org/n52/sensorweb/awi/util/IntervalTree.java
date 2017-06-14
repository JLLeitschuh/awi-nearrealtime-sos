package org.n52.sensorweb.awi.util;

import static java.util.stream.Collectors.toSet;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class IntervalTree<K, V> {

    private IntervalNode root;
    private final Comparator<? super K> comparator;

    public IntervalTree(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    public IntervalTree() {
        this.comparator = null;
    }

    @SuppressWarnings("unchecked")
    private int compare(K k1, K k2) {
        return comparator == null ? ((Comparable<? super K>) k1).compareTo(k2) : comparator.compare(k1, k2);
    }

    public void add(K min, K max, V value) {
        add(new Interval(min, max), value);
    }

    private void add(Interval element, V value) {
        IntervalNode x = root;
        IntervalNode node = new IntervalNode(element, value);
        K max = element.getUpper();

        if (root == null) {
            root = node;
            return;
        }

        while (true) {
            if (compare(node.getMax(), x.getMax()) > 0) {
                x.setMax(node.getMax());
            }

            int compare = element.compareTo(x.getKey());
            if (0 == compare) {
                return;
            } else if (compare < 0) {
                if (x.getLeft() == null) {
                    node.setParent(x);
                    x.setLeft(node);
                    x.decreaseBalanceFactor();
                    break;
                }
                x = x.getLeft();
            } else {
                if (x.getRight() == null) {
                    node.setParent(x);
                    x.setRight(node);
                    x.increaseBalanceFactor();
                    break;
                }
                x = x.getRight();
            }
        }

        rebalance(x);
    }

    private void recalculateMaxx(IntervalNode node) {
        recalculateMax(node.getLeft());
        recalculateMax(node.getRight());
        recalculateMax(node);
    }

    private K max(K a, K b) {
        return compare(a, b) > 0 ? a : b;
    }

    private void recalculateMax(IntervalNode node) {
        if (node == null) {
            return;
        }
        node.setMax(max(node.getKey().getUpper(),
                        Stream.of(node.getLeft(), node.getRight())
                                .filter(Objects::nonNull)
                                .map(IntervalNode::getMax)
                                .reduce(this::max)
                                .orElseGet(node.getKey()::getUpper)));
    }

    private void rotateRight(IntervalNode node) {
        IntervalNode y = node.getLeft();
        node.setLeft(y.getRight());
        if (node.getLeft() != null) {
            node.getLeft().setParent(node);
        }
        y.setParent(node.getParent());
        if (y.getParent() == null) {
            root = y;
        } else {
            if (node.getParent().getLeft() == node) {
                node.getParent().setLeft(y);
            } else {
                node.getParent().setRight(y);
            }
        }
        y.setRight(node);
        node.setParent(y);
    }

    private void rotateLeft(IntervalNode node) {
        IntervalNode y = node.getRight();
        node.setRight(y.getLeft());
        if (node.getRight() != null) {
            node.getRight().setParent(node);
        }
        y.setParent(node.getParent());
        if (y.getParent() == null) {
            root = y;
        } else {
            if (node.getParent().getLeft() == node) {
                y.getParent().setLeft(y);
            } else {
                y.getParent().setRight(y);
            }
        }
        y.setLeft(node);
        node.setParent(y);
    }

    private void rotateRightLeft(IntervalNode node) {
        rotateRight(node.getRight());
        rotateLeft(node);
    }

    private void rotateLeftRight(IntervalNode node) {
        rotateLeft(node.getLeft());
        rotateRight(node);
    }

    private IntervalNode minimumNode(IntervalNode node) {
        while (node.getLeft() != null) {
            node = node.getLeft();
        }
        return node;
    }

    private IntervalNode maxiumNode(IntervalNode node) {
        while (node.getRight() != null) {
            node = node.getRight();
        }
        return node;
    }

    private IntervalNode successor(IntervalNode node) {
        if (node.getRight() != null) {
            return minimumNode(node.getRight());
        }
        while (node.getParent() != null && node == node.getParent().getRight()) {
            node = node.getParent();
        }
        return node.getParent();
    }

    private IntervalNode predecessor(IntervalNode node) {
        if (node.getLeft() != null) {
            return maxiumNode(node.getLeft());
        }
        while (node.getParent() != null && node.getParent().getLeft() == node) {
            node = node.getParent();
        }
        return node.getParent();
    }

    public Set<V> searchInterval(Interval interval) {
        List<IntervalNode> found = new LinkedList<>();
        search(interval, root, found);
        return found.stream().map(IntervalNode::getValue).collect(toSet());
    }

    public Set<V> search(K lower, K upper) {
        return searchInterval(new Interval(lower, upper));
    }

    public Set<V> search(K key) {
        return search(key, key);
    }

    public Optional<V> get(K lower, K upper) {
        return searchNearest(new Interval(lower, upper)).map(IntervalNode::getValue);
    }

    public Optional<V> get(K key) {
        return get(key, key);
    }

    private void search(Interval key, IntervalNode node, List<IntervalNode> storage) {
        if (node == null) {
            return;
        }

        if (compare(node.getMax(), key.getLower()) < 0) {
            return;
        }

        search(key, node.getLeft(), storage);

        if (compare(key.getLower(), node.getKey().getUpper()) < 0 &&
            compare(node.getKey().getLower(), key.getUpper()) < 0) {
            storage.add(node);
        }

        if (compare(key.getUpper(), node.getKey().getLower()) < 0) {
            return;
        }

        search(key, node.getRight(), storage);
    }

    private Optional<IntervalNode> searchNearest(Interval key) {
        IntervalNode x = root;
        if (x == null) {
            return Optional.empty();
        }
        IntervalNode previous = x;
        int compare = 0;
        while (x != null) {
            previous = x;
            compare = key.compareTo(x.getKey());
            if (0 == compare) {
                return Optional.of(x);
            } else if (compare < 0) {
                x = x.getLeft();
            } else {
                x = x.getRight();
            }
        }

        x = (compare < 0) ? predecessor(previous) : successor(previous);
        if (x == null) {
            return Optional.of(previous);
        }
        int otherCompare = key.compareTo(x.getKey());
        if (compare < 0) {
            return Optional.of(Math.abs(compare) < otherCompare ? previous : x);
        } else {
            return Optional.of(Math.abs(otherCompare) < compare ? x : previous);
        }

    }

    private void rebalance(IntervalNode x) {
        IntervalNode node = x;
        while (node.getBalanceFactor() != 0 && node.getParent() != null) {
            if (node.getParent().getLeft() == node) {
                node.getParent().decreaseBalanceFactor();
            } else {
                node.getParent().increaseBalanceFactor();
            }

            node = node.getParent();
            if (node.getBalanceFactor() == 2) {
                if (node.getRight().getBalanceFactor() == 1) {
                    rotateLeft(node);
                    node.setBalanceFactor(0);
                    node.getParent().setBalanceFactor(0);
                    node = node.getParent();
                    recalculateMaxx(node);
                } else {
                    rotateRightLeft(node);
                    node = node.getParent();
                    recalculateMaxx(node);
                    switch (node.getBalanceFactor()) {
                        case 1:
                            node.getRight().setBalanceFactor(0);
                            node.getLeft().setBalanceFactor(-1);
                            break;
                        case 0:
                            node.getRight().setBalanceFactor(0);
                            node.getLeft().setBalanceFactor(0);
                            break;
                        default:
                            node.getRight().setBalanceFactor(1);
                            node.getLeft().setBalanceFactor(0);
                            break;
                    }
                    node.setBalanceFactor(0);
                }
                break;
            } else if (node.getBalanceFactor() == -2) {
                if (node.getLeft().getBalanceFactor() == -1) {
                    rotateRight(node);
                    node.setBalanceFactor(0);
                    node.getParent().setBalanceFactor(0);
                    node = node.getParent();
                    recalculateMaxx(node);
                } else {
                    rotateLeftRight(node);
                    node = node.getParent();
                    recalculateMaxx(node);
                    switch (node.getBalanceFactor()) {
                        case -1:
                            node.getRight().setBalanceFactor(1);
                            node.getLeft().setBalanceFactor(0);
                            break;
                        case 0:
                            node.getRight().setBalanceFactor(0);
                            node.getLeft().setBalanceFactor(0);
                            break;
                        default:
                            node.getRight().setBalanceFactor(0);
                            node.getLeft().setBalanceFactor(-1);
                            break;
                    }
                    node.setBalanceFactor(0);
                }
                break;
            }
        }
    }

    private class Interval implements Comparable<Interval> {

        private final K upper;
        private final K lower;

        Interval(K lower, K upper) {
            if (compare(lower, upper) > 0) {
                throw new IllegalArgumentException(String.format("Illegal interval: [%s,%s]", lower, upper));
            }
            this.upper = upper;
            this.lower = lower;
        }

        @Override
        public int compareTo(Interval other) {
            int comp = compare(getLower(), getLower());
            if (comp == 0) {
                comp = compare(getUpper(), getUpper());
            }
            return comp;
        }

        /**
         * @return the upper
         */
        K getUpper() {
            return upper;
        }

        /**
         * @return the lower
         */
        K getLower() {
            return lower;
        }
    }

    private class IntervalNode {
        private final Interval key;
        private final V value;
        private IntervalNode parent;
        private IntervalNode left;
        private IntervalNode right;
        private int balanceFactor;
        private K max;

        IntervalNode(Interval interval, V value) {
            this.key = interval;
            this.max = interval.getUpper();
            this.value = value;
        }

        /**
         * @return the key
         */
        protected Interval getKey() {
            return key;
        }

        /**
         * @return the value
         */
        protected V getValue() {
            return value;
        }

        /**
         * @return the parent
         */
        protected IntervalNode getParent() {
            return parent;
        }

        /**
         * @param parent the parent to set
         */
        protected void setParent(IntervalNode parent) {
            this.parent = parent;
        }

        /**
         * @return the left
         */
        protected IntervalNode getLeft() {
            return left;
        }

        /**
         * @param left the left to set
         */
        protected void setLeft(IntervalNode left) {
            this.left = left;
        }

        /**
         * @return the right
         */
        protected IntervalNode getRight() {
            return right;
        }

        /**
         * @param right the right to set
         */
        protected void setRight(IntervalNode right) {
            this.right = right;
        }

        /**
         * @return the balanceFactor
         */
        protected int getBalanceFactor() {
            return balanceFactor;
        }

        /**
         * @param balanceFactor the balanceFactor to set
         */
        protected void setBalanceFactor(int balanceFactor) {
            this.balanceFactor = balanceFactor;
        }

        void decreaseBalanceFactor() {
            this.balanceFactor--;
        }

        void increaseBalanceFactor() {
            this.balanceFactor++;
        }

        /**
         * @return the max
         */
        protected K getMax() {
            return max;
        }

        /**
         * @param max the max to set
         */
        protected void setMax(K max) {
            this.max = max;
        }
    }
}
