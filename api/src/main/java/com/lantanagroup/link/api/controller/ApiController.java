package com.lantanagroup.link.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.victools.jsonschema.generator.*;
import com.google.common.base.Strings;
import com.lantanagroup.link.Helper;
import com.lantanagroup.link.config.SwaggerConfig;
import com.lantanagroup.link.config.api.ApiConfig;
import com.lantanagroup.link.db.model.tenant.Tenant;
import com.lantanagroup.link.model.ApiInfoModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {
  @Autowired
  private ApiConfig apiConfig;

  @Autowired
  private SwaggerConfig swaggerConfig;

  @GetMapping(value = "/$tenant-schema", produces = {"text/plain"})
  public String getTenantJsonSchema() {
    SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
    SchemaGeneratorConfig config = configBuilder.build();
    SchemaGenerator generator = new SchemaGenerator(config);
    JsonNode jsonSchema = generator.generateSchema(Tenant.class);
    return jsonSchema.toPrettyString();
  }

  @GetMapping
  public ApiInfoModel getVersionInfo() {
    return Helper.getVersionInfo();
  }

  @GetMapping(value = "/docs", produces = "text/yaml")
  public String getDocs(HttpServletRequest request) throws IOException {
    ClassPathResource resource = new ClassPathResource("swagger.yml");
    InputStream inputStream = resource.getInputStream();
    String content = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));

    content += "\n  securitySchemes:\n" +
            "    oauth:\n" +
            "      type: oauth2\n" +
            "      flows:\n" +
            "        implicit:\n" +
            "          authorizationUrl: " + this.swaggerConfig.getAuthUrl() + "\n" +
            "          tokenUrl: " + this.swaggerConfig.getTokenUrl() + "\n" +
            "          scopes:\n";

    StringBuilder contentBuilder = new StringBuilder(content);
    for (String scope : this.swaggerConfig.getScope()) {
      contentBuilder.append(String.format("            %s: %s\n", scope, scope));
    }
    content = contentBuilder.toString();

    String publicAddress = this.getPublicAddress();

    content = content.replace("{{server-base-url}}",
            !Strings.isNullOrEmpty(publicAddress) ?
                    publicAddress.replace("/api", "") :
                    request.getRequestURL().toString().replace("/api/docs", "/"));

    ApiInfoModel apiInfoModel = this.getVersionInfo();
    content = content.replace("{{version}}", apiInfoModel != null && !Strings.isNullOrEmpty(apiInfoModel.getVersion()) ? apiInfoModel.getVersion() : "dev");

    inputStream.close();
    return content;
  }

  private String getPublicAddress() {
    if (Strings.isNullOrEmpty(this.apiConfig.getPublicAddress())) {
      return null;
    }

    if (this.apiConfig.getPublicAddress().endsWith("/")) {
      return this.apiConfig.getPublicAddress().substring(0, this.apiConfig.getPublicAddress().length() - 1);
    }

    return this.apiConfig.getPublicAddress();
  }
}
