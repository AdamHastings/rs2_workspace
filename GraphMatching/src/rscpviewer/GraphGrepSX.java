package rscpviewer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import edu.byu.ece.rapidSmith.design.subsite.Cell;
import edu.byu.ece.rapidSmith.design.subsite.CellDesign;
import edu.byu.ece.rapidSmith.design.subsite.CellNet;
import edu.byu.ece.rapidSmith.design.subsite.CellPin;
import edu.byu.ece.rapidSmith.interfaces.vivado.TincrCheckpoint;
import edu.byu.ece.rapidSmith.interfaces.vivado.VivadoInterface;

public class GraphGrepSX {
	public static void createFile(String input_filename) {
		TincrCheckpoint goldStandard;

		BufferedWriter bw = null;
		FileWriter fw = null;

		try {
			goldStandard = VivadoInterface.loadTCP(input_filename);
		} catch (IOException e) {
			System.out.println("File not found");
			e.printStackTrace();
			return;
		}

		CellDesign goldDesign = goldStandard.getDesign();
		Collection<Cell> cells_c_gs = goldDesign.getCells();
		ArrayList<Cell> goldCells = new ArrayList<Cell>();
		ArrayList<CellNet> goldNets = new ArrayList<CellNet>(goldDesign.getNets());
		HashSet<String> goldCellPins_set = new HashSet<String>();

		for (Cell c : cells_c_gs) {
			goldCells.add(c);
			Collection<CellPin> cellpins = c.getPins();
			
			for (CellPin pin : cellpins) {		
				goldCellPins_set.add(pin.getFullName());
			}
		}

		ArrayList<String> goldCellPins = new ArrayList<String>(goldCellPins_set);
		Collections.sort(goldCellPins, new Comparator<String>() {
			public int compare(String a, String b) {
				return a.compareTo(b);
			}
		});

		try {
			String filename = input_filename + "_ggsx";
			fw = new FileWriter(filename);
			bw = new BufferedWriter(fw);
			bw.write("#" + filename + "\n");
			bw.write(goldCellPins.size() + "\n");
			for (String pin : goldCellPins) {
				bw.write(pin + "\n");
			}
			
			
			// TODO QUESTION: Is is possible for a net to have two sources?
			// Answer: This shouldn't be a problem, i.e. it shouldn't be something that we encounter...
			int edge_count = 0;
			String edge_buffer = "";
			for (CellNet n : goldNets) {
				List<CellPin> sources = n.getAllSourcePins();
				if (sources.size() == 0) {
					System.out.println("WARNING: 0 sources");
				} else if (sources.size() == 1) {
					Collection<CellPin> sinks = n.getSinkPins();
					CellPin source = sources.get(0);
					int source_index = goldCellPins.indexOf(source.getFullName());
					for (CellPin sink : sinks) {
						int sink_index = goldCellPins.indexOf(sink.getFullName());
						edge_buffer += source_index + " " + sink_index + " " + n.getName() + "\n";
						edge_count = edge_count + 1;
					}
				} else {
					System.out.println("WARNING: 2+ sources");
					return; // Is this return necessary? How should multiple sources be handled?
				}
			}
			// TODO: I'm guessing we'll want to label the edges to help with speedup?
			// According the GraphGrepSX_v3.3/exs/db.geu file, it looks like the format for edges is
			// <source> <sink> <name>
			// Let's test this out though and test it just to be sure?
			bw.write(edge_count + "\n");
			bw.write(edge_buffer);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	/** Create a database or query file for GraphGrepSX3.3
	 * 	The function creates a file using the -ged format
	 *	(-ged => directed graph with labeled edges and vertices)
	 * 
	 * @param nets
	 * @param filename
	 */
	public static void createFile(Collection<CellNet> nets, String filename) {
		
		String moduleName = "aes_128_0" + "/";
		
		// Objects for file writing
		BufferedWriter bw = null;
		FileWriter fw = null;

		// A set to contain all pins connected to the nets
		HashSet<String> CellPins_set = new HashSet<String>();

		// Get all pins connected to all nets
		for (CellNet n : nets) {
			// Get all pins connected to this net
			if (n == null) {
				System.out.println("Null CellNet?");
			}
			Collection<CellPin> cellpins = n.getPins();
			
			// Add each of the pins to the goldcellPins_set
			for (CellPin pin : cellpins) {		
				CellPins_set.add(pin.getFullName());
			}
		}

		// Place the goldCellPins_set into a list
		ArrayList<String> goldCellPins = new ArrayList<String>(CellPins_set);
		// Sort the list in lexicographical order
		Collections.sort(goldCellPins, new Comparator<String>() {
			public int compare(String a, String b) {
				return a.compareTo(b);
			}
		});

		
		// Begin writing the output file
		try {
			// Write file header
			filename = filename + "_ggsx";
			fw = new FileWriter(filename);
			bw = new BufferedWriter(fw);
			bw.write("#" + filename + "\n");
			
			// Write the number of CellPins found
			bw.write(goldCellPins.size() + "\n");
			// Write each CellPin, in lexicographical order
			for (String pin : goldCellPins) {
				// Is this the best way to do this? Do you see any potential bugs from this approach?
				if (pin.startsWith(moduleName)) {
					pin = pin.replaceFirst("^" + moduleName, "");				
				}
				bw.write(pin + "\n");
			}
			
			int edge_count = 0;
			String edge_buffer = "";
			for (CellNet n : nets) {
				CellPin sources = n.getSourcePin();
				if (sources == null) {
					System.out.println("WARNING: 0 sources");
					System.out.println(n.toString());
					Collection<CellPin> sinks = n.getSinkPins();
					System.out.println(sinks.toString());
					System.out.println(n.getPins());
				} else {  // if (sources.size() == 1) {
					Collection<CellPin> sinks = n.getSinkPins();
					CellPin source = sources;
					int source_index = goldCellPins.indexOf(source.getFullName());
					for (CellPin sink : sinks) {
						
						// Again...are there any potential problems with doing this?
						String name = n.getName();
						if (name.startsWith(moduleName)) {
							name = name.replaceFirst("^" + moduleName, "");				
						}
						
						
						int sink_index = goldCellPins.indexOf(sink.getFullName());
						edge_buffer += source_index + " " + sink_index + " " + name + "\n";
						edge_count = edge_count + 1;
					}
				} /*else {
					System.out.println("WARNING: 2+ sources");
					return; // Is this return necessary? How should multiple sources be handled?
				}*/
			}
			
			bw.write(edge_count + "\n");
			bw.write(edge_buffer);
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Close the file
			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}

