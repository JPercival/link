package com.lantanagroup.nandina.query.fhir.r4.cerner;

import com.lantanagroup.nandina.query.BaseQuery;
import com.lantanagroup.nandina.query.fhir.r4.cerner.filter.Filter;
import com.lantanagroup.nandina.query.fhir.r4.cerner.report.PreviousDayAdmissionConfirmedCovidReport;
import com.lantanagroup.nandina.query.fhir.r4.cerner.scoop.EncounterScoop;
import org.hl7.fhir.r4.model.Resource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PreviousDayAdmissionConfirmedCovidQuery extends BaseQuery {
    @Override
    public Integer execute() {
        if (!this.criteria.containsKey("reportDate") && !this.criteria.containsKey("overflowLocations")) {
            return null;
        }

        EncounterScoop encounterScoop = (EncounterScoop) this.getContextData("scoopData");
        List<Filter> filters = new ArrayList<Filter>();
        PreviousDayAdmissionConfirmedCovidReport onsetReport = new PreviousDayAdmissionConfirmedCovidReport(encounterScoop, filters, java.sql.Date.valueOf(LocalDate.parse(this.criteria.get("reportDate"))));
        this.addContextData("previousDayCovidAdmitted", onsetReport.getReportCount());

        return onsetReport.getReportCount();
    }

    @Override
    protected Map<String, Resource> queryForData() {
        return null;
    }
}
