package com.lantanagroup.link.config.sender;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FhirSenderUrlOAuthConfig {

  FHIRSenderOAuthConfig authConfig;
  /**
   * <strong>oauth.url</strong>
   */
  private String url;


}