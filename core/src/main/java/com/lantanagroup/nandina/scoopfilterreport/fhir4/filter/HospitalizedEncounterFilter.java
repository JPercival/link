package com.lantanagroup.nandina.scoopfilterreport.fhir4.filter;

import java.util.Date;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;

import com.lantanagroup.nandina.scoopfilterreport.fhir4.PatientData;

public class HospitalizedEncounterFilter extends Filter {

	@Override
	public boolean runFilter(PatientData pd) {
		return hasHospitalizedEncounterClass(pd);
	}
	
	private boolean hasHospitalizedEncounterClass(PatientData pd) {
		boolean b = false;
		for (IBaseResource res : bundleToSet(pd.getEncounters())) {
			Encounter enc = (Encounter)res;
			Coding c = enc.getClass_();
			if (
					c.getCode().equals("IMP")
					|| c.getCode().equals("ACUTE")
					|| c.getCode().equals("NONAC")
					|| c.getCode().equals("OBSENC")
					) b = true;
			// TODO check Encounter.class to see if one of IMP,ACUTE,NONAC,OBSENC
		}
		return b;
	}

}