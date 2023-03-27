package com.lantanagroup.link;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.apache.GZipContentInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.param.TokenParam;
import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.config.api.ApiDataStoreConfig;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class FhirDataProvider {
  private static final Logger logger = LoggerFactory.getLogger(FhirDataProvider.class);
  private static final String PeriodStartParamName = "periodStart";
  private static final String PeriodEndParamName = "periodEnd";
  private static final String ReportDocRefSystem = "urn:ietf:rfc:3986";
  protected FhirContext ctx = FhirContextProvider.getFhirContext();

  @Getter
  private IGenericClient client;

  public FhirDataProvider(IGenericClient client) {
    this.client = client;
  }

  public FhirDataProvider(ApiDataStoreConfig config) {
    IGenericClient client = FhirContextProvider.getFhirContext().newRestfulGenericClient(config.getBaseUrl());
    if (StringUtils.isNotEmpty(config.getUsername()) && StringUtils.isNotEmpty(config.getPassword())) {
      BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor(config.getUsername(), config.getPassword());
      client.registerInterceptor(authInterceptor);
    }
    this.client = client;
  }

  public FhirDataProvider(String fhirBase) {
    this.client = this.ctx.newRestfulGenericClient(fhirBase);
    this.client.registerInterceptor(new GZipContentInterceptor());
  }

  public Resource createResource(IBaseResource resource) {
    MethodOutcome outcome = this.client
            .create()
            .resource(resource)
            .execute();

    if (!outcome.getCreated() || outcome.getResource() == null) {
      logger.error("Failed to store/create FHIR resource");
    } else {
      logger.debug("Stored FHIR resource with new " + outcome.getId().toString());
    }

    return (Resource) outcome.getResource();
  }

  public MethodOutcome updateResource(IBaseResource resource) {
    int initialVersion = resource.getMeta().getVersionId() != null ? Integer.parseInt(resource.getMeta().getVersionId()) : 0;

    // Make sure the ID is not version-specific
    if (resource.getIdElement() != null && resource.getIdElement().getIdPart() != null) {
      resource.setId(resource.getIdElement().getIdPart());
    }

    MethodOutcome outcome = this.client
            .update()
            .resource(resource)
            .execute();

    Resource domainResource = (Resource) outcome.getResource();
    int updatedVersion = Integer.parseInt(outcome.getId().getVersionIdPart());
    if (updatedVersion > initialVersion) {
      logger.debug(String.format("Update is successful for %s/%s", domainResource.getResourceType().toString(), domainResource.getIdElement().getIdPart()));
    } else {
      logger.info(String.format("Nothing changed in resource %s/%s", domainResource.getResourceType().toString(), domainResource.getIdElement().getIdPart()));
    }

    return outcome;
  }

  public DocumentReference findDocRefByMeasuresAndPeriod(Collection<Identifier> identifiers, String periodStart, String periodEnd) throws Exception {
    DocumentReference documentReference = null;
    DateClientParam periodStartParam = new DateClientParam(PeriodStartParamName);
    DateClientParam periodEndParam = new DateClientParam(PeriodEndParamName);
    List<IQueryParameterType> identifierParams = identifiers.stream()
            .map(identifier -> new TokenParam(identifier.getSystem(), identifier.getValue()))
            .collect(Collectors.toList());

    Bundle bundle = this.client
            .search()
            .forResource(DocumentReference.class)
            .where(Map.of(DocumentReference.SP_IDENTIFIER, identifierParams))
            .and(periodStartParam.exactly().second(periodStart))
            .and(periodEndParam.exactly().second(periodEnd))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    int size = bundle.getEntry().size();
    if (size > 0) {
      if (size == 1) {
        documentReference = (DocumentReference) bundle.getEntry().get(0).getResource();
      } else {
        throw new Exception("We have more than 1 report for the selected measures and report date.");
      }
    }

    return documentReference;
  }

  public DocumentReference findDocRefForReport(String reportId) {
    Bundle bundle = this.client
            .search()
            .forResource(DocumentReference.class)
            .where(DocumentReference.IDENTIFIER.exactly().systemAndValues(ReportDocRefSystem, reportId))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    if (bundle.getEntry().size() != 1) {
      return null;
    }

    return (DocumentReference) bundle.getEntryFirstRep().getResource();
  }

  public Bundle findListByIdentifierAndDate(String system, String value, String start, String end) {
    Bundle bundle = this.client
            .search()
            .forResource(ListResource.class)
            .where(ListResource.IDENTIFIER.exactly().systemAndValues(system, value))
            .and(new DateClientParam("applicable-period-start").exactly().second(start))
            .and(new DateClientParam("applicable-period-end").exactly().second(end))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
    return bundle;
  }

  public MeasureReport getMeasureReportById(String reportId) {
    MeasureReport report = this.client
            .read()
            .resource(MeasureReport.class)
            .withId(reportId)
            .execute();

    return report;
  }

  public Bundle getBundleById(String bundleId) {
    Bundle report = this.client
            .read()
            .resource(Bundle.class)
            .withId(bundleId)
            .execute();

    return report;
  }

  public Bundle transaction(Bundle txBundle) {
    logger.trace("Executing transaction on " + this.client.getServerBase());

    Bundle responseBundle = this.client
            .transaction()
            .withBundle(txBundle)
            .execute();

    return responseBundle;
  }

  public Measure findMeasureByIdentifier(Identifier identifier) {
    Bundle measureBundle = this.client.search()
            .forResource("Measure")
            .where(Measure.IDENTIFIER.exactly().systemAndIdentifier(identifier.getSystem(), identifier.getValue()))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();

    if (measureBundle.getEntry().size() != 1) {
      return null;
    }

    return (Measure) measureBundle.getEntryFirstRep().getResource();
  }

  public void audit(HttpServletRequest request, LinkCredentials user, FhirHelper.AuditEventTypes type, String outcomeDescription) {
    AuditEvent auditEvent = FhirHelper.createAuditEvent(request, user, type, outcomeDescription);
    this.createResource(auditEvent);
  }

  /**
   * Gets a complete resource by retrieving it based on type and id
   *
   * @param resourceType
   * @param resourceId
   * @return
   */
  public IBaseResource getResourceByTypeAndId(String resourceType, String resourceId) {
    return this.client
            .read()
            .resource(resourceType)
            .withId(resourceId)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
  }

  public MeasureReport getMeasureReport(String measureId, Parameters parameters) {
    MeasureReport measureReport = client.operation()
            .onInstance(new IdType("Measure", measureId))
            .named("$evaluate-measure")
            .withParameters(parameters)
            .returnResourceType(MeasureReport.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
    return measureReport;
  }

  public MethodOutcome createOutcome(IBaseResource resource) {
    return this.client
            .create()
            .resource(resource)
            .prettyPrint()
            .encodedJson()
            .execute();
  }

  public String bundleToXml(Bundle bundle) {
    return client.getFhirContext().newXmlParser().encodeResourceToString(bundle);
  }

  public String bundleToJson(Bundle bundle) {
    return client.getFhirContext().newJsonParser().encodeResourceToString(bundle);
  }

  public Bundle searchReportDefinition(String system, String value) {
    return client.search()
            .forResource(Bundle.class)
            .withTag(Constants.MainSystem, Constants.ReportDefinitionTag)
            .where(Bundle.IDENTIFIER.exactly().systemAndCode(system, value))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
  }

  public Bundle searchPractitioner(String practitionerId) {
    return client
            .search()
            .forResource(Practitioner.class)
            .withTag(Constants.MainSystem, Constants.LinkUserTag)
            .where(Practitioner.IDENTIFIER.exactly().systemAndValues(Constants.MainSystem, practitionerId))
            .returnBundle(Bundle.class)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
  }

  public IBaseResource retrieveFromServer(String resourceType, String resourceId) {
    return client
            .read()
            .resource(resourceType)
            .withId(resourceId)
            .cacheControl(new CacheControlDirective().setNoCache(true))
            .execute();
  }

  public Bundle getAllResourcesByType(Class<? extends IBaseResource> classType) {
    return client
            .search()
            .forResource(classType)
            .returnBundle(Bundle.class)
            .execute();
  }

  public void deleteResource(String resourceType, String id, boolean permanent) {
    try {
      URL url = new URL(this.client.getServerBase() + "/" + resourceType + "/" + id + (permanent?"?_expunge=true":""));
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("DELETE");
      con.getResponseMessage();
      con.disconnect();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
