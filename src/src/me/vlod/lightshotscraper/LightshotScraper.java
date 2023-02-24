package me.vlod.lightshotscraper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class LightshotScraper implements Runnable {
	public static LightshotScraper instance;
	public boolean running;
	public String outputFolderBase;
	public boolean noSaving;
	public boolean noDownload;
	
	/** 
	 * Splits a string by spaces, ignoring quotes<br>
	 * From <a href="https://stackoverflow.com/a/366532">https://stackoverflow.com/a/366532</a>
	 * 
	 * @param str the string to split
	 * @return the result or empty array
	 */
	public static String[] splitBySpace(String str) {
		try {
			List<String> matchList = new ArrayList<String>();
			Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
			Matcher regexMatcher = regex.matcher(str);
			
			while (regexMatcher.find()) {
			    if (regexMatcher.group(1) != null) {
			        // Add double-quoted string without the quotes
			        matchList.add(regexMatcher.group(1));
			    } else if (regexMatcher.group(2) != null) {
			        // Add single-quoted string without the quotes
			        matchList.add(regexMatcher.group(2));
			    } else {
			        // Add unquoted word
			        matchList.add(regexMatcher.group());
			    }
			}	
			
			return matchList.toArray(new String[0]);
		} catch (Exception ex) {
			ex.printStackTrace();
			return new String[0];
		}
	}
	
	public String[] generateLinks(int amount) {
		String chars = "abcdefghijklmnoprqstuwvxyz";
		ArrayList<String> links = new ArrayList<String>();
		ThreadLocalRandom random = ThreadLocalRandom.current();
		
		for (int i = 0; i < amount; i++) {
			String link = "https://prnt.sc/";
			
			for (int j = 0; j < 2; j++) {
				link += chars.charAt(random.nextInt(0, chars.length() - 1));
			}
			
			link += "" + random.nextInt(0, 9);
			link += "" + random.nextInt(0, 9);
			link += "" + random.nextInt(0, 9);
			link += "" + random.nextInt(0, 9);
			
			links.add(link);
		}
		
		return links.toArray(new String[0]);
	}
	
	public void downloadImage(String url, String outputFilePath) {
		try {
			Response resultImageResponse = Jsoup.connect(url).ignoreContentType(true).execute();
			FileOutputStream imageStream = new FileOutputStream(new File(outputFilePath));
			imageStream.write(resultImageResponse.bodyAsBytes());
			imageStream.close();	
		} catch (Exception ex) {
			if (ex instanceof HttpStatusException && 
				((HttpStatusException)ex).getStatusCode() == 404) {
				System.err.println(String.format("Link \"%s\" doesn't exist!", url));
			} else {
				System.err.println(String.format("Unable to download the image from the link \"%s\"!", url));
				ex.printStackTrace();
			}
		}
	}
	
	@Override
	public void run() {
		this.running = true;
		Scanner inputScanner = new Scanner(System.in);
		
		System.out.println("Welcome to LightshotScraper!");
		System.out.println("Type \"help\" for more information");
		System.out.println("");
		
		while (this.running) {
			System.out.print("> ");
			
			String input = inputScanner.nextLine();
			String[] inputSplitted = LightshotScraper.splitBySpace(input);
			
			String cmd = inputSplitted[0];
			String[] arguments = new String[inputSplitted.length - 1];
			System.arraycopy(inputSplitted, 1, arguments, 0, arguments.length);
			
            for (int argIndex = 0; argIndex < arguments.length; argIndex++) {
                String arg = arguments[argIndex];

                if (arg.startsWith("\"")) {
                	arg = arg.substring(1, arg.length() - 1);
                }
                    
                if (arg.endsWith("\"")) {
                	arg = arg.substring(0, arg.length() - 1);
                }
                
                arguments[argIndex] = arg;
            }

			switch (cmd) {
			case "generate":
				if (arguments.length < 1) {
					System.out.println("Not enough arguments!");
					break;
				}
				
				String numberRaw = arguments[0];
				int number = 0;
				
				try {
					number = Integer.valueOf(numberRaw);
				} catch (Exception ex) {
					System.out.println("Invalid arguments provided!");
					break;
				}
				
				long timeBeforeGen = System.currentTimeMillis();
				String outputFolderPath = "";
				
				if (!this.noSaving) {
					outputFolderPath = (this.outputFolderBase == null ? 
							"." : this.outputFolderBase.trim()) + "/" + timeBeforeGen;
					System.out.println(String.format("Output folder: %s", outputFolderPath));
					
					// Create the output folder if it doesn't exist
					try {
						File outputFolder = new File(outputFolderPath);
						if (!outputFolder.exists()) {
							outputFolder.mkdir();
						}	
					} catch (Exception ex) {
						System.err.println("Unable to create the output folder! Do we have not write permissions?");
						System.err.println("Disabled file saving");
						this.noSaving = true;
						ex.printStackTrace();
					}
				}

				String[] links = this.generateLinks(number);
				PrintWriter fileDump = null;
				if (!this.noSaving) {
					try {
						fileDump = new PrintWriter(new FileOutputStream(
								new File(String.format("%s/links.html", outputFolderPath))));
						fileDump.println("<!Doctype HTML>");
						fileDump.println("<html>");
						fileDump.println("\t<body>");
						fileDump.println("\t\t<span>");
					} catch (Exception ex) {
						System.err.println("Unable to create the HTML file! Do we have not write permissions?");
						ex.printStackTrace();
					}	
				}

				for (int i = 0; i < links.length; i++) {
					String link = links[i];
					String linkImageURL = "";
					
					// Print to console
					System.out.println(String.format("(%d) %s", i, link));
					
					if (!this.noSaving) {
						// Get image URL
						try {
							Document document = Jsoup.connect(link).get();
							Element element = document.getElementById("screenshot-image");
							linkImageURL = element.attr("src");
						} catch (Exception ex) {
							System.err.println(
									String.format("Unable to get the image URL for the link %s!", linkImageURL));
							ex.printStackTrace();
						}
						
						// Write to file
						if (fileDump != null) {
							fileDump.println(String.format(
									"\t\t\t(%d) <a href=\"%s\" target=_blank>%s</a>:<br>", i, link, link));
							fileDump.println(String.format(
									"\t\t\t<img style=\"color: red;\" src=\"%s\" alt=\"Link %d doesn\'t exist!\"/><br>", linkImageURL, i));
						}
						
						// Download image URL
						if (linkImageURL != "" && !this.noDownload) {
							this.downloadImage(linkImageURL, String.format("%s/%d.png", 
									outputFolderPath, i));
						}	
					}
				}
				
				if (fileDump != null) {
					fileDump.println("\t\t</span>");
					fileDump.println("\t</body>");
					fileDump.println("</html>");
					fileDump.flush();
					fileDump.close();
				}
				
				long timeAfterGen = System.currentTimeMillis();
				System.out.println(String.format("Generated" + (this.noSaving || this.noDownload ? " " : " & downloaded ") + "%d links in %d seconds", 
						links.length, (timeAfterGen - timeBeforeGen) / 1000));
				
				break;
			case "togglenosaving":
				this.noSaving = !this.noSaving;
				System.out.println(String.format("No saving is now %s", this.noSaving));
				break;
			case "togglenodownload":
				this.noDownload = !this.noDownload;
				System.out.println(String.format("No download is now %s", this.noDownload));
				break;
			case "setoutputfolderbase":
				if (arguments.length < 1) {
					System.out.println("Not enough arguments!");
					break;
				}
				
				this.outputFolderBase = arguments[0];
				System.out.println(String.format("Set output folder base to %s", this.outputFolderBase));
				
				break;
			case "exit":
				this.running = false;
				break;
			case "help":
				System.out.println("generate <number> - Generates x amount of links");
				System.out.println("setoutputfolderbase <path> - Sets the output folder base");
				System.out.println("togglenosaving - Toggles no saving");
				System.out.println("togglenodownload - Toggles no downloading");
				System.out.println("exit");
				break;
			default:
				System.out.println("Invalid command!");
				break;
			}
		}
		
		inputScanner.close();
	}
	
	public static void main(String[] args) {
		new Thread(instance = new LightshotScraper()).start();
	}
}
