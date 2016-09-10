package models;

import java.util.LinkedHashMap;
import java.util.Set;

public class Gene {
	
	private String name;
	private int totalTrinucleotide;
	private int totalDinucleotide;
	
	// Trinucleotide
	private LinkedHashMap<String, Integer> trinuStatPhase0;
	private LinkedHashMap<String, Double> trinuProbaPhase0;

	private LinkedHashMap<String, Integer> trinuStatPhase1;
	private LinkedHashMap<String, Double> trinuProbaPhase1;

	private LinkedHashMap<String, Integer> trinuStatPhase2;
	private LinkedHashMap<String, Double> trinuProbaPhase2;
	
	// Dinucleotide
	private LinkedHashMap<String, Integer> dinuStatPhase0;
	private LinkedHashMap<String, Double> dinuProbaPhase0;

	private LinkedHashMap<String, Integer> dinuStatPhase1;
	private LinkedHashMap<String, Double> dinuProbaPhase1;
	
	public Gene(String name, int totalDinucleotide, int totalTrinucleotide)
	{
		this.name = name;
		this.totalTrinucleotide = totalDinucleotide;
		this.totalDinucleotide = totalTrinucleotide;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTotalTrinucleotide() {
		return totalTrinucleotide;
	}

	public void setTotalTrinucleotide(int totalTrinucleotide) {
		this.totalTrinucleotide = totalTrinucleotide;
	}

	public int getTotalDinucleotide() {
		return totalDinucleotide;
	}

	public void setTotalDinucleotide(int totalDinucleotide) {
		this.totalDinucleotide = totalDinucleotide;
	}

	public LinkedHashMap<String, Integer> getTrinuStatPhase0() {
		return trinuStatPhase0;
	}

	public void setTrinuStatPhase0(LinkedHashMap<String, Integer> trinuStatPhase0) {
		this.trinuStatPhase0 = trinuStatPhase0;
	}

	public LinkedHashMap<String, Double> getTrinuProbaPhase0() {
		return trinuProbaPhase0;
	}

	public void setTrinuProbaPhase0(LinkedHashMap<String, Double> trinuProbaPhase0) {
		this.trinuProbaPhase0 = trinuProbaPhase0;
	}

	public LinkedHashMap<String, Integer> getTrinuStatPhase1() {
		return trinuStatPhase1;
	}

	public void setTrinuStatPhase1(LinkedHashMap<String, Integer> trinuStatPhase1) {
		this.trinuStatPhase1 = trinuStatPhase1;
	}

	public LinkedHashMap<String, Double> getTrinuProbaPhase1() {
		return trinuProbaPhase1;
	}

	public void setTrinuProbaPhase1(LinkedHashMap<String, Double> trinuProbaPhase1) {
		this.trinuProbaPhase1 = trinuProbaPhase1;
	}

	public LinkedHashMap<String, Integer> getTrinuStatPhase2() {
		return trinuStatPhase2;
	}

	public void setTrinuStatPhase2(LinkedHashMap<String, Integer> trinuStatPhase2) {
		this.trinuStatPhase2 = trinuStatPhase2;
	}

	public LinkedHashMap<String, Double> getTrinuProbaPhase2() {
		return trinuProbaPhase2;
	}

	public void setTrinuProbaPhase2(LinkedHashMap<String, Double> trinuProbaPhase2) {
		this.trinuProbaPhase2 = trinuProbaPhase2;
	}

	public LinkedHashMap<String, Integer> getDinuStatPhase0() {
		return dinuStatPhase0;
	}

	public void setDinuStatPhase0(LinkedHashMap<String, Integer> dinuStatPhase0) {
		this.dinuStatPhase0 = dinuStatPhase0;
	}

	public LinkedHashMap<String, Double> getDinuProbaPhase0() {
		return dinuProbaPhase0;
	}

	public void setDinuProbaPhase0(LinkedHashMap<String, Double> dinuProbaPhase0) {
		this.dinuProbaPhase0 = dinuProbaPhase0;
	}

	public LinkedHashMap<String, Integer> getDinuStatPhase1() {
		return dinuStatPhase1;
	}

	public void setDinuStatPhase1(LinkedHashMap<String, Integer> dinuStatPhase1) {
		this.dinuStatPhase1 = dinuStatPhase1;
	}

	public LinkedHashMap<String, Double> getDinuProbaPhase1() {
		return dinuProbaPhase1;
	}

	public void setDinuProbaPhase1(LinkedHashMap<String, Double> dinuProbaPhase1) {
		this.dinuProbaPhase1 = dinuProbaPhase1;
	}
}
