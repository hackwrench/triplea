package games.strategy.thread;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;

/**
 * Utility class for ensuring that locks are acquired in a consistent order.
 * <p>
 * Simply use this class and call acquireLock(aLock) releaseLock(aLock) instead of lock.lock(), lock.release(). If locks
 * are acquired in an
 * inconsistent order, an error message will be printed.
 * <p>
 * This class is not terribly good for multithreading as it locks globally on all calls, but that is ok, as this code is
 * meant more for when
 * you are considering your ambitious multi-threaded code a mistake, and you are trying to limit the damage.
 * <p>
 */
public class LockUtil {
  // the locks the current thread has
  // because locks can be re-entrant, store this as a count
  private final static ThreadLocal<Map<Lock, Integer>> m_locksHeld = new ThreadLocal<>();
  // a map of all the locks ever held when a lock was acquired
  // store weak references to everything so that locks don't linger here forever
  private final static Map<Lock, Set<WeakLockRef>> m_locksHeldWhenAcquired = new WeakHashMap<>();
  private final Object m_mutex = new Object();
  private static ErrorReporter m_errorReporter = new ErrorReporter();

  public LockUtil() {}

  public void acquireLock(final Lock aLock) {
    synchronized (m_mutex) {
      if (m_locksHeld.get() == null) {
        m_locksHeld.set(new HashMap<>());
      }
      // we already have the lock, increaase the count
      if (m_locksHeld.get().containsKey(aLock)) {
        final int current = m_locksHeld.get().get(aLock);
        m_locksHeld.get().put(aLock, current + 1);
      }
      // we don't have it
      else {
        // all the locks currently held must be acquired before a lock
        if (!m_locksHeldWhenAcquired.containsKey(aLock)) {
          m_locksHeldWhenAcquired.put(aLock, new HashSet<>());
        }
        for (final Lock l : m_locksHeld.get().keySet()) {
          m_locksHeldWhenAcquired.get(aLock).add(new WeakLockRef(l));
        }
        // we are lock a, check to
        // see if any lock we hold (b)
        // has evern been acquired before a
        for (final Lock l : m_locksHeld.get().keySet()) {
          final Set<WeakLockRef> held = m_locksHeldWhenAcquired.get(l);
          // clear out of date locks
          final Iterator<WeakLockRef> iter = held.iterator();
          while (iter.hasNext()) {
            if (iter.next().get() == null) {
              iter.remove();
            }
          }
          if (held.contains(new WeakLockRef(aLock))) {
            m_errorReporter.reportError(aLock, l);
          }
        }
        m_locksHeld.get().put(aLock, 1);
      }
    }
    aLock.lock();
  }

  public void releaseLock(final Lock aLock) {
    synchronized (m_mutex) {
      int count = m_locksHeld.get().get(aLock);
      count--;
      if (count == 0) {
        m_locksHeld.get().remove(aLock);
      } else {
        m_locksHeld.get().put(aLock, count);
      }
    }
    aLock.unlock();
  }

  public boolean isLockHeld(final Lock aLock) {
    if (m_locksHeld.get() == null) {
      return false;
    }
    synchronized (m_mutex) {
      return m_locksHeld.get().containsKey(aLock);
    }
  }

  public void setErrorReporter(final ErrorReporter reporter) {
    m_errorReporter = reporter;
  }

  public static class ErrorReporter {
    public void reportError(final Lock from, final Lock to) {
      System.err.println("Invalid lock ordering at, from:" + from + " to:" + to + " stack trace:" + getStackTrace());
    }

    private String getStackTrace() {
      final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      final StringBuilder builder = new StringBuilder();
      for (final StackTraceElement e : trace) {
        builder.append(e.toString());
        builder.append("\n");
      }
      return builder.toString();
    }
  }
  protected static final class WeakLockRef extends WeakReference<Lock> {
    // cache the hash code to make sure it doesn't change if our reference
    // has been cleared
    private final int hashCode;

    public WeakLockRef(final Lock referent) {
      super(referent);
      hashCode = referent.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof WeakLockRef) {
        final WeakLockRef other = (WeakLockRef) o;
        return other.get() == this.get();
      }
      return false;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }
  }
}
