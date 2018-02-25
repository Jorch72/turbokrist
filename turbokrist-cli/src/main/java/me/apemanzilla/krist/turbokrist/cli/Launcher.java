package me.apemanzilla.krist.turbokrist.cli;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import me.apemanzilla.krist.state.NodeState;
import me.lignum.jkrist.Krist;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.nativelibs4java.opencl.CLDevice;

import me.apemanzilla.krist.turbokrist.MinerOptions;
import me.apemanzilla.krist.turbokrist.miners.MinerFactory;
import me.apemanzilla.krist.turbokrist.miners.MinerInitException;

/**
 * Command-line launcher for turbokrist OpenCL miner
 * 
 * @author apemanzilla
 *
 */
public class Launcher {
	private static final String NAME_META_REGEX = "^(?:([a-z0-9-_]{1,32})@)?([a-z0-9]{1,64})\\.kst$";

	private static Options options = new Options();

	static {
		options.addOption(Option.builder("h").longOpt("host").hasArg().argName("address")
				.desc("The Krist address to mine for").build());
		options.addOption(Option.builder("p").longOpt("privatekey").hasArg().argName("privatekey")
				.desc("The privatekey for relay mode.").build());
		options.addOption(Option.builder().longOpt("relay")
				.desc("Mine to a temporary address before sending to the host address.").build());
//		options.addOption(
//				Option.builder("p").longOpt("profiler").desc("Start the system profiler to optimize mining").build());
		options.addOption(
				Option.builder("l").longOpt("list-devices").desc("Show a list of compatible devices").build());
		options.addOption(Option.builder("w").longOpt("work-sizes").hasArg().argName("work_sizes")
				.desc("Set the work sizes per device type.").build());
		options.addOption(
				Option.builder("a").longOpt("all-devices").desc("Use all compatible devices for mining.").build());
		options.addOption(Option.builder("b").longOpt("best-device")
				.desc("Use only the best device for mining, determined by number of compute units. Default device selector.")
				.build());
		options.addOption(Option.builder("d").longOpt("devices").hasArg().argName("devices")
				.desc("Specify which devices to use by ID.").build());
		options.addOption(Option.builder("v").longOpt("verbose").desc("Enable verbose logging").build());
		options.addOption(Option.builder("r").longOpt("refresh-rate").hasArg().argName("milliseconds")
				.desc("Sets the refresh rate for checking for block/work changes").build());
		options.addOption(Option.builder("n").longOpt("node").hasArg().argName("nodeURL")
				.desc("The URL of the Krist node to mine for.").build());
		options.addOption(Option.builder("?").longOpt("help").desc("Show command-line usage").build());
	}

	public static void printHelp() {
		HelpFormatter hf = new HelpFormatter();
		// show in order defined
		hf.setOptionComparator(null);
		hf.printHelp("turbokrist -h <address>", options);
	}

	public static void listDevices() {
		Map<Integer, CLDevice> devices = MinerFactory.getDeviceIDs();
		System.out.format("%20s | %11s | %4s\n", "Name", "Signature", "ID");
		Set<Entry<Integer, CLDevice>> es = devices.entrySet();
		for (Entry<Integer, CLDevice> e : es) {
			System.out.format("%20s | %11s | %4s\n", e.getValue().getName().trim(),
					MinerFactory.getSignature(e.getValue()), e.getKey());
		}
	}

	public static void main(String[] args) throws ParseException, MinerInitException {
		CommandLine cmd = new DefaultParser().parse(options, args);
		boolean verbose = cmd.hasOption('v');
		if (cmd.hasOption('l')) {
			if (verbose)
				System.out.println("Listing compatible OpenCL devices");
			listDevices();
			System.exit(1);
//		} else if (cmd.hasOption('p')) {
//			if (verbose)
//				System.out.println("Running system profiler");
//			// TODO: run profiler
//			System.exit(1);
		} else if (cmd.hasOption('?')) {
			if (verbose)
				System.out.println("Come on, do you really need verbose output when looking at the help?");
			printHelp();
			System.exit(1);
		}
		if (!cmd.hasOption("h")) {
			System.out.println("Please specify an address with -h.");
			System.exit(1);
		}

		if (cmd.hasOption("n")) {
			String nodeURL = cmd.getOptionValue('n');
			if (verbose)
				System.out.println("Using Krist node " + nodeURL);
			NodeState.setKrist(new Krist(nodeURL));
		} else {
			if (verbose)
				System.out.println("Using default Krist node (krist.ceriat.net)");
			NodeState.setKrist(new Krist());
		}

		MinerOptions options = new MinerOptions();
		options.setDepositAddress(cmd.getOptionValue('h'));
		
		if (options.getDepositAddress().matches(NAME_META_REGEX)) {
			if (verbose)
				System.out.println("Name detected. Relay enabled - mining to a temporary address.");
			options.setRelay(true);
		}
		
		if (cmd.hasOption("a")) {
			if (verbose)
				System.out.println("Selecting all devices.");
			options.selectAllDevices();
		} else if (cmd.hasOption("d")) {
			if (verbose)
				System.out.println("Selecting given devices.");
			String[] split = cmd.getOptionValue("d").split(",");
			int[] ids = new int[split.length];
			for (int i = 0; i < split.length; i++) {
				ids[i] = Integer.parseInt(split[i]);
			}
			options.selectDevices(ids);
		} else {
			if (verbose)
				System.out.println("Selecting best device.");
			options.selectBestDevice();
		}
		if (cmd.hasOption("w")) {
			if (verbose)
				System.out.println("Setting device worksizes.");
			String[] sizes = cmd.getOptionValue("w").split(";");
			for (String size : sizes) {
				int sig = Integer.parseInt(size.split(":")[0]);
				int work = Integer.parseInt(size.split(":")[1]);
				options.setWorkSize(sig, work);
			}
		}
		if (cmd.hasOption("r")) {
			if (verbose)
				System.out.println("Setting refresh rate.");
			options.setStateRefreshRate(Integer.parseInt(cmd.getOptionValue("r")));
		}
		if (cmd.hasOption("p")) {
			String privatekey = cmd.getOptionValue('p');
			if (verbose)
				System.out.println("Using custom privatekey.");
			options.setPrivatekey(privatekey);
		}
		if (cmd.hasOption("relay")) {
			if (verbose)
				System.out.println("Relay enabled - mining to a temporary address.");
			options.setRelay(true);
		}

		System.out.println("Starting miner...");
		Controller controller = new Controller(options);
		controller.start();
	}

}
