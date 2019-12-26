package com.univocity.trader.cli;

import com.univocity.trader.*;
import com.univocity.trader.candles.*;
import com.univocity.trader.config.*;
import com.univocity.trader.simulation.*;
import com.univocity.trader.tickers.Ticker.*;
import com.univocity.trader.tickers.*;
import nonapi.io.github.classgraph.utils.*;
import org.apache.commons.cli.*;

import java.util.*;

public class Main {
	/**
	 * configuration file option
	 */
	private static final String CONFIG_OPTION = "config";
	/**
	 * exchange option
	 */
	private static final String EXCHANGE_OPTION = "exchange";
	/**
	 * backfill historical data option
	 */
	private static final String BACKFILL_OPTION = "backfill";
	/**
	 * simulation option
	 */
	private static final String SIMULATE_OPTION = "simulate";
	/**
	 * live trading option
	 */
	private static final String TRADE_OPTION = "trade";

	private static String[] getPairs(Exchange exchange) {
		final String[] univocitySymbols = Tickers.getInstance().getSymbols(Type.crypto);
		final String[] univocityReference = Tickers.getInstance().getSymbols(Type.reference);
		final String[] univocityPairs = Tickers.getInstance().makePairs(univocitySymbols, univocityReference);
		final Map<String, SymbolInformation> symbolInfo = exchange.getSymbolInformation();
		final List<String> lst = new ArrayList<String>();
		for (final String pair : univocityPairs) {
			if (symbolInfo.containsKey(pair)) {
				lst.add(pair);
			}
		}
		final String[] ret = new String[lst.size()];
		lst.toArray(ret);
		return ret;
	}

	public static void main(String... args) {
		System.out.println("Univocity CLI");
		/*
		 * options
		 */
		final Options options = new Options();
		Option oo = Option.builder().argName(CONFIG_OPTION).longOpt(CONFIG_OPTION).type(String.class).hasArg().required(false).desc("configuration file").build();
		options.addOption(oo);
		oo = Option.builder().argName(EXCHANGE_OPTION).longOpt(EXCHANGE_OPTION).type(String.class).hasArg().required(true).desc("exchange name").build();
		options.addOption(oo);
		oo = Option.builder().argName(BACKFILL_OPTION).longOpt(BACKFILL_OPTION).hasArg(false).required(false).desc("backfill historical data loaded from the exchange").build();
		options.addOption(oo);
		oo = Option.builder().argName(SIMULATE_OPTION).longOpt(SIMULATE_OPTION).hasArg(false).required(false).desc("simulate").build();
		options.addOption(oo);
		oo = Option.builder().argName(TRADE_OPTION).longOpt(TRADE_OPTION).hasArg(false).required(false).desc("trade live on the given exchange").build();
		options.addOption(oo);
		/*
		 * parse
		 */
		final CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
			/*
			 * config
			 */
//			final String configFileName = cmd.getOptionValue(CONFIG_OPTION);
//			if (null != configFileName) {
//				Configuration.load(configFileName);
//			} else {
//				Configuration.load();
//			}
			/*
			 * TODO we might want to fix this....
			 */
			final String exchangeName = cmd.getOptionValue(EXCHANGE_OPTION);
			EntryPoint entryPoint = Utils.findClassAndInstantiate(EntryPoint.class, exchangeName);

			if (cmd.hasOption(TRADE_OPTION)) {
				livetrade(entryPoint);
			} else {
				AbstractMarketSimulator<?, ?> simulator = loadSimulator(entryPoint);

				/*
				 * run command
				 */
				if (cmd.hasOption(BACKFILL_OPTION)) {
					/*
					 * update market history
					 */
					simulator.updateHistory();
				}
				if (cmd.hasOption(SIMULATE_OPTION)) {
					/*
					 * simulate
					 */
					simulator.run();
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("posix", options);
		}
	}

	private static void livetrade(EntryPoint entryPoint) {
		var trader = (LiveTrader<?, ?, ?>) ReflectionUtils.invokeMethod(entryPoint, "trader", true);
		trader.run();
	}

	private static AbstractMarketSimulator<?, ?> loadSimulator(EntryPoint entryPoint) {
		return (AbstractMarketSimulator<?, ?>) ReflectionUtils.invokeMethod(entryPoint, "simulator", true);
	}
}