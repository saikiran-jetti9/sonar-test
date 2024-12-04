package com.bmg.trigon.service;

import com.bmg.trigon.common.enums.Territory;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BigqueryService {

  private final BigQuery bigQuery;

  @Value("${spring.cloud.gcp.bigquery.project-id}")
  private String bigQueryProjectId;

  public BigqueryService(BigQuery bigQuery) {
    this.bigQuery = bigQuery;
  }

  public Long getLastRoyaltyRunNumberForPublishing(Territory territory)
      throws InterruptedException {
    String query =
        "SELECT RUN_NUMBER FROM "
            + bigQueryProjectId
            + ".prime_publishing_royalties_dummy.imdfaafl \n"
            + "where DFAA_SYS_TERR='"
            + territory.name()
            + "'\n"
            + "and RUN_NUMBER is not null order by RUN_NUMBER desc limit 1";

    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(query).setMaxResults(1L).build();
    TableResult tableResult = bigQuery.query(queryConfig);

    return tableResult.iterateAll().iterator().next().get(0).getLongValue();
  }

  public String getLastRoyaltyRunDateForRecorded(Territory territory) throws InterruptedException {
    String query =
        "select mod_date from "
            + bigQueryProjectId
            + ".prime_recorded.calclog where calclog_runtype='I'\n"
            + "and calclog_site='"
            + territory.name()
            + "'\n"
            + "and calclog_status='Succeeded' and mod_date is not null order by mod_date desc limit 1";

    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(query).setMaxResults(1L).build();
    TableResult tableResult = bigQuery.query(queryConfig);

    return tableResult.iterateAll().iterator().next().get(0).getStringValue();
  }
}
