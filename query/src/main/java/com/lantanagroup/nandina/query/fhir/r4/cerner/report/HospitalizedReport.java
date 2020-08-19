package com.lantanagroup.nandina.query.fhir.r4.cerner.report;

import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.CovidFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.HospitalizedEncounterFilter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;

import java.util.ArrayList;
import java.util.List;

public class HospitalizedReport extends Report {
	
	public HospitalizedReport(EncounterScoop scoop) {
		super(scoop, addFilters(new ArrayList<Filter>()));
	}


	public HospitalizedReport(EncounterScoop scoop, List<Filter> filters) {
		super(scoop, addFilters(filters));
	}

	private static List<Filter> addFilters(List<Filter> filters) {
		Filter covidFilter = new CovidFilter();
		filters.add(covidFilter);
		Filter hospitalizedFilter = new HospitalizedEncounterFilter();
		filters.add(hospitalizedFilter);
		return filters;
	}

}