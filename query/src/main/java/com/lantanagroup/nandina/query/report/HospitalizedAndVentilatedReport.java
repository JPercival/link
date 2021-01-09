package com.lantanagroup.nandina.query.report;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.nandina.query.filter.Filter;
import com.lantanagroup.nandina.query.filter.HospitalizedEncounterFilter;
import com.lantanagroup.nandina.query.filter.VentilatedFilter;
import com.lantanagroup.nandina.query.scoop.EncounterScoop;

import java.util.List;

public class HospitalizedAndVentilatedReport extends HospitalizedReport {

  public HospitalizedAndVentilatedReport(EncounterScoop scoop, List<Filter> filters, FhirContext ctx) {
    super(scoop, addFilters(filters), ctx);
    // TODO Auto-generated constructor stub
  }


  private static List<Filter> addFilters(List<Filter> filters) {
    Filter ventilatedFilter = new VentilatedFilter();
    filters.add(ventilatedFilter);
    Filter hospitalizedFilter = new HospitalizedEncounterFilter();
    filters.add(hospitalizedFilter);
    return filters;
  }

}