/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.remoting.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for background thread
 */
public abstract class ServiceThread implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(RemotingHelper.ROCKETMQ_REMOTING);

    /**
     * 等待过期时间
     */
    private static final long JOIN_TIME = 90 * 1000;

    protected final Thread thread;

    /**
     * 调用状态
     */
    protected volatile boolean hasNotified = false;

    /**
     * 停止状态
     */
    protected volatile boolean stopped = false;

    /**
     * 构造器
     */
    public ServiceThread() {
        this.thread = new Thread(this, this.getServiceName());
    }

    /**
     * 获取service的名字
     * @return
     */
    public abstract String getServiceName();

    /**
     * 线程启动方法
     */
    public void start() {
        this.thread.start();
    }

    /**
     * 非中断式,关闭
     */
    public void shutdown() {
        this.shutdown(false);
    }

    /**
     * 关闭线程
     * @param interrupt
     */
    public void shutdown(final boolean interrupt) {
        this.stopped = true;
        log.info("shutdown thread " + this.getServiceName() + " interrupt " + interrupt);
        synchronized (this) {
            // 唤醒当前线程
            if (!this.hasNotified) {
                this.hasNotified = true;
                this.notify();
            }
        }

        try {
            // 中断线程
            if (interrupt) {
                this.thread.interrupt();
            }

            long beginTime = System.currentTimeMillis();
            // 超时join
            this.thread.join(this.getJointime());
            long eclipseTime = System.currentTimeMillis() - beginTime;
            log.info("join thread " + this.getServiceName() + " eclipse time(ms) " + eclipseTime + " "
                + this.getJointime());
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        }
    }

    public long getJointime() {
        return JOIN_TIME;
    }

    public boolean isStopped() {
        return stopped;
    }
}
