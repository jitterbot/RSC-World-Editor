package com.openrsc.server.database.impl.mysql;

import com.openrsc.server.Server;
import com.openrsc.server.database.GameLogger;
import com.openrsc.server.database.impl.mysql.queries.Query;
import com.openrsc.server.database.impl.mysql.queries.ResultQuery;
import com.openrsc.server.util.ServerAwareThreadFactory;
import com.openrsc.server.util.SystemUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MySqlGameLogger extends GameLogger {

	   
                            
    
	private static final Logger LOGGER = LogManager.getLogger();

	private volatile AtomicBoolean running;
	private final BlockingQueue<Query> queries;
	private final Server server;
	private ScheduledExecutorService scheduledExecutor;
	private final MySqlGameDatabase database;

	public MySqlGameLogger(final Server server, final MySqlGameDatabase database) {
		this.server = server;

		running = new AtomicBoolean(false);
		queries = new ArrayBlockingQueue<>(10000);
		                                                       
		if (database == null) {
			LOGGER.error("GameDatabase provided was null or not a MySqlGameDatabase.");
			SystemUtil.exit(1);
		}
		this.database = database;
	}

	public final Server getServer() {
		return server;
	}

	public void start() {
		synchronized (running) {
			running.set(true);
			scheduledExecutor = Executors.newSingleThreadScheduledExecutor(
					new ServerAwareThreadFactory(
							server.getName() + " : DatabaseLogging",
							server.getConfig()
					)
			);
			scheduledExecutor.scheduleAtFixedRate(this, 0, 50, TimeUnit.MILLISECONDS);
		}
	}

	public void stop() {
		synchronized (running) {
			scheduledExecutor.shutdown();
			try {
				final boolean terminationResult = scheduledExecutor.awaitTermination(1, TimeUnit.MINUTES);
				if (!terminationResult) {
					LOGGER.error("MySqlGameLogger thread termination failed");
					List<Runnable> skippedTasks = scheduledExecutor.shutdownNow();
					LOGGER.error("{} task(s) never commenced execution", skippedTasks.size());
				}
			} catch (final InterruptedException e) {
				LOGGER.error("MySqlGameLogger thread interrupted during stop()");
			}
			clearQueries();
			scheduledExecutor = null;
			running.set(false);
		}
	}

	private void clearQueries() {
		queries.clear();
	}

	@Override
	public void run() {
		synchronized (running) {
			if (running.get()) {
				while (queries.size() > 0 && getDatabase().getConnection().isConnected()) {
					pollNextQuery();
				}
			}
		}
	}

	protected void pollNextQuery() {
		runQuery(queries.poll());
	}

	protected void runQuery(final Query query) {
		try {
			if (query != null) {
				if (query instanceof ResultQuery) {
					final ResultQuery rq = (ResultQuery) query;
					try (final PreparedStatement statement = rq.prepareStatement(getDatabase().getConnection().getConnection());
						 final ResultSet result = statement.executeQuery();) {
						rq.onResult(result);
					}
				} else {
					try (final PreparedStatement statement = query.prepareStatement(getDatabase().getConnection().getConnection());) {
						statement.execute();
					}
				}
			}
		                                            
                       
    } catch (final Exception ex) {
			LOGGER.error("Error executing runQuery", ex);
			if (server.getDiscordService() != null && server.getConfig().WANT_DISCORD_GENERAL_LOGGING) {
				server.getDiscordService().errorLogStackTrace(ex);
			}
		}
	}

	                                                                                                                                              
	public void addQuery(final Query query) {
		if (!running.get()) {
			return;
		}
		queries.add(query);
	}

	                                                                                                                                              
	public void run(final Query query) {
		runQuery(query);
	}

	private MySqlGameDatabase getDatabase() {
		return database;
	}
}
