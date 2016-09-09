package models;

import java.util.LinkedHashMap;
import java.util.Set;

public class Gene {
	
	public String name;
	public int totalTrinucleotide;
	public int totalDinucleotide;
	
	// Trinucleotide
	public LinkedHashMap<String, Integer> trinuStatPhase0;
	public LinkedHashMap<String, Double> trinuProbaPhase0;
	
	public LinkedHashMap<String, Integer> trinuStatPhase1;
	public LinkedHashMap<String, Double> trinuProbaPhase1;
	
	public LinkedHashMap<String, Integer> trinuStatPhase2;
	public LinkedHashMap<String, Double> trinuProbaPhase2;
	
	// Dinucleotide
	public LinkedHashMap<String, Integer> dinuStatPhase0;
	public LinkedHashMap<String, Double> dinuProbaPhase0;
	
	public LinkedHashMap<String, Integer> dinuStatPhase1;
	public LinkedHashMap<String, Double> dinuProbaPhase1;
	
	public Gene()
	{
		this.name = "";
		this.totalTrinucleotide = 0;
		this.totalDinucleotide = 0;
		
		this.trinuStatPhase0 = Cds.initLinkedHashMap();
		this.trinuStatPhase1 = Cds.initLinkedHashMap();
		this.trinuStatPhase2 = Cds.initLinkedHashMap();
		
		this.trinuProbaPhase0 = Cds.initLinkedHashMapProba();
		this.trinuProbaPhase1 = Cds.initLinkedHashMapProba();
		this.trinuProbaPhase2 = Cds.initLinkedHashMapProba();
		
		this.dinuStatPhase0 = Cds.initLinkedHashMapDinucleo();
		this.dinuStatPhase1 = Cds.initLinkedHashMapDinucleo();
		
		this.dinuProbaPhase0 = Cds.initLinkedHashMapDinucleoProba();
		this.dinuProbaPhase1 = Cds.initLinkedHashMapDinucleoProba();
	}

}
