package de.jowo.pspac;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import de.jowo.pspac.remote.ProgressMonitor;
import de.jowo.pspac.remote.dto.ProgressInfo;

public class LoggingProgressMonitor implements ProgressMonitor {
	private static final Logger logger = Logger.getLogger(LoggingProgressMonitor.class);

	final Lock lock = new ReentrantLock();
	final Condition workerFinished = lock.newCondition();

	private final long workerId;
	private Thread thread;

	private ProgressInfo latestInfo;

	public LoggingProgressMonitor(long workerId) {
		this.workerId = workerId;
	}

	public void setThread(Thread thread) {
		this.thread = thread;
	}

	@Override
	public void reportProgress(ProgressInfo info) {
		this.latestInfo = info;

		switch (info.getStatus()) {
			case ACTIVE:
				logger.debug(String.format("[%d] %d %% - %s", workerId, info.getPercentage(), info.getMessage()));
				break;
			case ERROR:
				String msg = "Execution on worker failed with exception";
				if (info.getMessage() instanceof Exception) {
					logger.error(msg, (Exception) info.getMessage());
				} else {
					logger.error(msg + ": " + info.getMessage());
				}
				break;
			case FINISHED:
				lock.lock();
				workerFinished.signal();
				lock.unlock();
				break;
			default:
				break;
		}
	}

	public long getWorkerId() {
		return workerId;
	}

	public ProgressInfo getLatestInfo() {
		return latestInfo;
	}

	public Thread getThread() {
		return thread;
	}

	@Override
	public String toString() {
		return String.format("[%d] latest = '%s'", workerId, latestInfo == null ? "null" : latestInfo.toString());
	}
}
