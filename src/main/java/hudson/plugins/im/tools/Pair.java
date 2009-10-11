package hudson.plugins.im.tools;

/**
 * <code>Pair</code> is a helper class for the frequent case, when two objects
 * must be combined for a collection entry or even a key. The concept of a pair
 * is fundamental for all languages derived from LISP and is even useful in a
 * language like Java.
 * <p>
 * Note: <code>Pair</code>s may be used as keys for a hash collection, when
 * both elements, named head and tail, implements the hashing protocol.
 *
 * @param <H>
 *            Type of the head
 * @param <T>
 *            Type of the tail
 */
public final class Pair<H, T> {
    /** Head of the pair. */
    private final H head;
    /** Tail of the pair. */
    private final T tail;

    /**
     * Constructs a <code>Pair</code> using the two given objects as head and
     * tail.
     *
     * @param head
     *            the object to be used as the head of the pair
     * @param tail
     *            the object to be used as the tail of the pair
     */
    public Pair(final H head, final T tail) {
        this.head = head;
        this.tail = tail;
    }
    
    public static <H2, T2> Pair<H2, T2> create(H2 head, T2 tail) {
        return new Pair<H2, T2>(head, tail);
    }

    /**
     * Returns the head object of the pair.
     *
     * @return the head object of the pair
     * @see #getTail
     */
    public H getHead() {
        return head;
    }

    /**
     * Returns the tail object of the pair.
     *
     * @return the tail object of the pair
     * @see #getHead
     */
    public T getTail() {
        return tail;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        else if ((other != null) && (other.getClass() == getClass())) {
            return compareContents((Pair<?, ?>)other);
        }
        else {
            return false;
        }
    }

    /**
     * Compares the contents of this pair with the other pair.
     *
     * @param other
     *            the other pair
     * @return <code>true</code> if the contents are equal, <code>false</code>
     *         otherwise
     */
    private boolean compareContents(final Pair<?, ?> other) {
        if (head == null) {
            if (other.head != null) {
                return false;
            }
        }
        else if (!head.equals(other.head)) {
            return false;
        }

        if (tail == null) {
            if (other.tail != null) {
                return false;
            }
        }
        else if (!tail.equals(other.tail)) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return ((head == null) ? 0 : head.hashCode()) + ((tail == null) ? 0 : tail.hashCode());
    }
}

