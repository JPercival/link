package com.lantanagroup.link.db.repositories;

import com.lantanagroup.link.db.model.PatientId;
import com.lantanagroup.link.db.model.PatientList;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.UUID;

public class PatientListRepository extends BaseRepository<PatientList> {
  private final DataSource dataSource;

  public PatientListRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  @SneakyThrows(SQLException.class)
  protected PatientList mapOne(ResultSet resultSet) {
    PatientList model = new PatientList();
    model.setId(resultSet.getObject("id", UUID.class));
    model.setMeasureId(resultSet.getNString("measureId"));
    model.setPeriodStart(resultSet.getNString("periodStart"));
    model.setPeriodEnd(resultSet.getNString("periodEnd"));
    model.setPatients(deserializeList(PatientId.class, resultSet.getNString("patients")));
    model.setLastUpdated(resultSet.getTimestamp("lastUpdated"));
    return model;
  }

  @SneakyThrows(SQLException.class)
  public List<PatientList> findAll() {
    String sql = "SELECT * FROM dbo.patientList;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {
      return mapAll(resultSet);
    }
  }

  @SneakyThrows(SQLException.class)
  public PatientList findById(UUID id) {
    String sql = "SELECT * FROM dbo.patientList WHERE id = ?;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, id);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? mapOne(resultSet) : null;
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public List<PatientList> findByReportId(String reportId) {
    String sql = "SELECT PL.* " +
            "FROM dbo.patientList AS PL " +
            "INNER JOIN dbo.reportPatientList AS RPL ON PL.id = RPL.patientListId " +
            "WHERE RPL.reportId = ?;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setNString(1, reportId);
      try (ResultSet resultSet = statement.executeQuery()) {
        return mapAll(resultSet);
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public PatientList findByMeasureIdAndReportingPeriod(String measureId, String periodStart, String periodEnd) {
    String sql = "SELECT * FROM dbo.patientList WHERE measureId = ? AND periodStart = ? AND periodEnd = ?;";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setNString(1, measureId);
      statement.setNString(2, periodStart);
      statement.setNString(3, periodEnd);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? mapOne(resultSet) : null;
      }
    }
  }

  private int insert(PatientList patientList, Connection connection) throws SQLException {
    String sql = "INSERT INTO dbo.patientList " +
            "(id, measureId, periodStart, periodEnd, patients, lastUpdated) " +
            "VALUES " +
            "(?, ?, ?, ?, ?, ?);";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(patientList, statement);
      parameters.addId();
      parameters.addMeasureId();
      parameters.addPeriodStart();
      parameters.addPeriodEnd();
      parameters.addPatients();
      parameters.addLastUpdated();
      return statement.executeUpdate();
    }
  }

  private int update(PatientList patientList, Connection connection) throws SQLException {
    String sql = "UPDATE dbo.patientList " +
            "SET patients = ?, lastUpdated = ? " +
            "WHERE measureId = ? AND periodStart = ? AND periodEnd = ?;";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      Parameters parameters = new Parameters(patientList, statement);
      parameters.addPatients();
      parameters.addLastUpdated();
      parameters.addMeasureId();
      parameters.addPeriodStart();
      parameters.addPeriodEnd();
      return statement.executeUpdate();
    }
  }

  @SneakyThrows(SQLException.class)
  public void save(PatientList patientList) {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        if (update(patientList, connection) == 0) {
          insert(patientList, connection);
        }
        connection.commit();
      } catch (Exception e) {
        connection.rollback();
        throw e;
      }
    }
  }

  @SneakyThrows(SQLException.class)
  public void deleteByIds(List<UUID> ids) {
    String sql = "DELETE FROM dbo.patientList WHERE id = ?;";
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        for (UUID id : ids) {
          statement.setObject(1, id);
          statement.executeUpdate();
        }
        connection.commit();
      } catch (Exception e) {
        connection.rollback();
        throw e;
      }
    }
  }

  private class Parameters {
    private final PatientList model;
    private final PreparedStatement statement;
    private int nextParameterIndex = 1;

    public Parameters(PatientList model, PreparedStatement statement) {
      this.model = model;
      this.statement = statement;
    }

    public void addId() throws SQLException {
      statement.setObject(nextParameterIndex++, model.getId());
    }

    public void addMeasureId() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getMeasureId());
    }

    public void addPeriodStart() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getPeriodStart());
    }

    public void addPeriodEnd() throws SQLException {
      statement.setNString(nextParameterIndex++, model.getPeriodEnd());
    }

    public void addPatients() throws SQLException {
      statement.setNString(nextParameterIndex++, serializeList(model.getPatients()));
    }

    public void addLastUpdated() throws SQLException {
      statement.setTimestamp(nextParameterIndex++, new Timestamp(model.getLastUpdated().getTime()));
    }
  }
}
