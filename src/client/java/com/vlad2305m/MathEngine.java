package com.vlad2305m;

import static com.vlad2305m.ChatqalcClient.LOGGER;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

import org.jetbrains.annotations.Contract;

public class MathEngine {

	private static Process qalc;
	public static Consumer<String> addMessage = (s) -> {
	};
	
	private static ArrayList<String> qalcAnswerBuffer = new ArrayList<>();
	
	private static String qalcBin = null;
	
	private static File local = new File("./config/chatqalc/qalc_bin_location");
	private static File windows = new File("C://Program Files/Qalculate/qalc.exe");
	private static File linux = new File("/usr/bin/qalc");

	private static String getQalcFile() {
		if (qalcBin == null) {
			
			if (windows.exists()) {
				qalcBin = windows.getPath();
			} else if (linux.exists()) {
				qalcBin = linux.getPath();
			} else if (local.exists()) {
				try {
					Scanner s = new Scanner(local);
					qalcBin = s.nextLine();
					s.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		return qalcBin;
	}

	public static void initMathEngine() {
		if (qalc != null && qalc.isAlive())
			return;
		try {

            MathEngineInstaller.install();
            
            if (getQalcFile() == null) 
            	return;

			ProcessBuilder pb = new ProcessBuilder(getQalcFile());

			Map<String, String> env = pb.environment();
			env.put("QALCULATE_USER_DIR", "./config/chatqalc");

			qalc = pb.start();

			BufferedReader reader = qalc.inputReader();

			new Thread(() -> readLoop(reader, qalc)).start();

		} catch (IOException e) {
			LOGGER.error(e.toString());
		}
	}

	private static boolean checkQalcDown() {
		if (qalc == null) {
			addMessage.accept("Error: Could not start qalc. Is Qalculate installed?");
			return true;
		}
		if (!qalc.isAlive()) {
			addMessage.accept("Warning: qalc down. Restarting...");
			initMathEngine();
		}
		if (!qalc.isAlive()) {
			addMessage.accept("Error: Could not start qalc. Is Qalculate installed?");
			return true;
		}
		return false;
	}

	@Contract(value = "!null->_", pure = false)
	public static void eval(String input) {
		if (getQalcFile() == null) {
			if (new File(input).exists()) {
				try {
					local.createNewFile();
					BufferedWriter writer = new BufferedWriter(new FileWriter(local));
					writer.write(input);
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				initMathEngine();
				if (qalc != null && qalc.isAlive()) {
					addMessage.accept("Success");					
				} else {					
					addMessage.accept("could not start qalc with this path");
					local.delete();
					qalcBin = null;
				}
			} else {
				addMessage.accept("Qalculate executable not found. Please enter the path to the qalc binary (qalc.exe) and confirm with Ctrl+Enter");
			}
			return;
		}
		if (checkQalcDown())
			return;

		BufferedWriter writer = qalc.outputWriter();
		try {
			writer.write(input + "\n");
			writer.flush();
		} catch (IOException e) {
			LOGGER.error(e.toString());
		}
		awaitAndSendAnswerBuffer();
	}

	@Contract(value = "_->_", pure = false)
	public static void tabComp(String input) {
		if (checkQalcDown())
			return;

		BufferedWriter writer = qalc.outputWriter();
		try {
			writer.write(input + "\n");
			writer.flush();
			writer.write("\t\n");
			writer.flush();
		} catch (IOException e) {
			LOGGER.error(e.toString());
		}
	}

	@Contract(value = "!null->_", pure = false)
	public static void evalSingle(String input) {

		try {
			ProcessBuilder pb = new ProcessBuilder(getQalcFile(), "-t", input);
			Map<String, String> env = pb.environment();
			env.put("QALCULATE_USER_DIR", "./config/chatqalc");
			Process qalc2 = pb.start();
			BufferedReader reader = qalc2.inputReader();
			new Thread(() -> readLoop(reader, qalc2)).start();
		} catch (IOException e) {
			LOGGER.error(e.toString());
		}
		awaitAndSendAnswerBuffer();
	}
	
	public static void awaitAndSendAnswerBuffer() {
		while (qalcAnswerBuffer.isEmpty()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		for (int i = 0; i < qalcAnswerBuffer.size(); i++) {
			String line = qalcAnswerBuffer.get(i);
			if (line != null)
				addMessage.accept(line);
		}
		qalcAnswerBuffer.clear();
	}

	private static void readLoop(BufferedReader reader, Process process) {
		while (process.isAlive()) {
			try {
				String message = reader.readLine();
				qalcAnswerBuffer.add(message);
			} catch (Throwable e) {
				LOGGER.error(e.toString());
			}
		}
	}

	public static String reformatAnsiMinecraft(String in) {
		return in.replace("\033[0m", "§r") // reset
				.replace("\033[1m", "§l") // bold
				.replace("\033[3m", "§o") // italic
				.replace("\033[4m", "§n") // underline
				.replace("\033[8m", "§k") // obfuscated
				.replace("\033[9m", "§m") // strikethrough
				.replace("\033[23m", "§r") // end italic -> reset
				.replace("\033[0;30m", "§0") // black
				.replace("\033[0;34m", "§1") // blue
				.replace("\033[0;32m", "§2") // green
				.replace("\033[0;36m", "§3") // cyan
				.replace("\033[0;31m", "§4") // red
				.replace("\033[0;35m", "§5") // purple
				.replace("\033[0;33m", "§6") // gold
				.replace("\033[0;37m", "§7") // gray
				.replace("\033[0;90m", "§8") // D grey
				.replace("\033[0;94m", "§9") // B blue
				.replace("\033[0;92m", "§a") // B green
				.replace("\033[0;96m", "§b") // B cyan
				.replace("\033[0;91m", "§c") // B red
				.replace("\033[0;95m", "§d") // B purple
				.replace("\033[0;93m", "§e") // B yellow
				.replace("\033[0;97m", "§f") // white
		// .replace("\033[m", "§") //
		;
	}

	public static String stripAnsi(String in) {
		return in.replace("\033[0m", "") //
				.replace("\033[1m", "") //
				.replace("\033[3m", "") //
				.replace("\033[4m", "") //
				.replace("\033[8m", "") //
				.replace("\033[9m", "") //
				.replace("\033[23m", "") //
				.replace("\033[0;30m", "") //
				.replace("\033[0;34m", "") //
				.replace("\033[0;32m", "") //
				.replace("\033[0;36m", "") //
				.replace("\033[0;31m", "") //
				.replace("\033[0;35m", "") //
				.replace("\033[0;33m", "") //
				.replace("\033[0;37m", "") //
				.replace("\033[0;90m", "") //
				.replace("\033[0;94m", "") //
				.replace("\033[0;92m", "") //
				.replace("\033[0;96m", "") //
				.replace("\033[0;91m", "") //
				.replace("\033[0;95m", "") //
				.replace("\033[0;93m", "") //
				.replace("\033[0;97m", "") //
				.replace("−", "-")
		// .replace("\033[m", "§") //
		;
	}

	public static String reformatMinecraftAnsi(String in) {
		return in.replace("§r", "\033[0m") // reset
				.replace("§l", "\033[1m") // bold
				.replace("§o", "\033[3m") // italic
				.replace("§n", "\033[4m") // underline
				.replace("§k", "\033[8m") // obfuscated
				.replace("§m", "\033[9m") // strikethrough
				.replace("§0", "\033[0;30m") // black
				.replace("§1", "\033[0;34m") // blue
				.replace("§2", "\033[0;32m") // green
				.replace("§3", "\033[0;36m") // cyan
				.replace("§4", "\033[0;31m") // red
				.replace("§5", "\033[0;35m") // purple
				.replace("§6", "\033[0;33m") // gold
				.replace("§7", "\033[0;37m") // gray
				.replace("§8", "\033[0;90m") // D grey
				.replace("§9", "\033[0;94m") // B blue
				.replace("§a", "\033[0;92m") // B green
				.replace("§b", "\033[0;96m") // B cyan
				.replace("§c", "\033[0;91m") // B red
				.replace("§d", "\033[0;95m") // B purple
				.replace("§e", "\033[0;93m") // B yellow
				.replace("§f", "\033[0;97m") // white
		;
	}
}
