/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/**
 * {@link Runnable} which represent a one time task which may allow the {@link EventExecutor} to reduce the amount of
 * produced garbage when queue it for execution.
 *
 * <strong>It is important this will not be reused. After submitted it is not allowed to get submitted again!</strong>
 */
public abstract class OneTimeTask implements Runnable {

    private static final long nextOffset;
    private static final sun.misc.Unsafe unsafe = getUnsafe();

    private static sun.misc.Unsafe getUnsafe() {
        try {
            return sun.misc.Unsafe.getUnsafe();
        } catch (SecurityException se) {
            try {
                return java.security.AccessController.doPrivileged
                        (new java.security
                                .PrivilegedExceptionAction<sun.misc.Unsafe>() {
                            @Override
                            public sun.misc.Unsafe run() throws Exception {
                                java.lang.reflect.Field f = sun.misc
                                        .Unsafe.class.getDeclaredField("theUnsafe");
                                f.setAccessible(true);
                                return (sun.misc.Unsafe) f.get(null);
                            }});
            } catch (java.security.PrivilegedActionException e) {
                throw new RuntimeException("Could not initialize intrinsics",
                        e.getCause());
            }
        }
    }

    static {
        try {
            nextOffset = unsafe.objectFieldOffset(
                    OneTimeTask.class.getDeclaredField("tail"));
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    @SuppressWarnings("unused")
    private volatile OneTimeTask tail;

    // Only use from MpscLinkedQueue and so we are sure Unsafe is present
    @SuppressWarnings("unchecked")
    final OneTimeTask next() {
        return (OneTimeTask) unsafe.getObjectVolatile(this, nextOffset);
    }

    // Only use from MpscLinkedQueue and so we are sure Unsafe is present
    final void setNext(final OneTimeTask newNext) {
        unsafe.putOrderedObject(this, nextOffset, newNext);
    }
}
