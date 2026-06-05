/********************************************************************************
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.sde.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the current Flyway-managed PostgreSQL schema and seed state.
 *
 * <p>The tests run migrations against a real PostgreSQL Testcontainer and inspect tables, columns,
 * constraints, indexes, and seeded role-permission data. This protects the persistence contract that
 * repositories and security checks depend on, and it also documents current schema gaps explicitly so
 * later migration work can update the assertions with a clear before-and-after signal.</p>
 */
@Testcontainers
class FlywaySchemaCurrentStateTest {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15.4")
			.withDatabaseName("sde_flyway_current_state")
			.withUsername("sde")
			.withPassword("sde");

	private static DataSource dataSource;

	@BeforeAll
	static void migrateFreshDatabase() {
		PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
		pgDataSource.setUrl(POSTGRES.getJdbcUrl());
		pgDataSource.setUser(POSTGRES.getUsername());
		pgDataSource.setPassword(POSTGRES.getPassword());
		dataSource = pgDataSource;

		Flyway.configure()
				.dataSource(dataSource)
				.locations("classpath:/flyway")
				.baselineOnMigrate(true)
				.load()
				.migrate();
	}

	@Test
	void flywayCreatesCoreRuntimeTablesOnFreshPostgres() throws Exception {
		assertThat(existingTables()).contains(
				"aspect",
				"aspect_relationship",
				"batch",
				"contract_negotiation_info",
				"failure_log",
				"process_report",
				"sde_permission",
				"sde_role",
				"sde_role_permission_mapping");
	}

	@Test
	void flywayCurrentStateDocumentsJpaTablesThatAreNotCreatedByVisibleMigrations() throws Exception {
		assertThat(existingTables()).doesNotContain(
				"consumer_download_history",
				"pcf_requests_tbl",
				"pcf_response_tbl",
				"policy_tbl");
	}

	@Test
	void userRoleCurrentlyHasRoleManagementPermissions() throws Exception {
		try (Connection connection = dataSource.getConnection();
				var statement = connection.prepareStatement("""
						SELECT sde_permission
						FROM sde_role_permission_mapping
						WHERE sde_role = 'User'
						ORDER BY sde_permission
						""")) {
			ResultSet resultSet = statement.executeQuery();

			assertThat(readSingleColumn(resultSet)).contains("create_role", "read_role_permission", "delete_role");
		}
	}

	@Test
	void flywayCreatesCurrentProcessReportPolicyColumns() throws Exception {
		assertThat(existingColumns("process_report")).contains(
				"access_policies",
				"usage_policies",
				"process_id",
				"reference_process_id",
				"policy_uuid");
	}

	@Test
	void flywayCreatesCurrentContractNegotiationColumns() throws Exception {
		assertThat(existingColumns("contract_negotiation_info")).contains(
				"id",
				"process_id",
				"offer_id",
				"contract_negotiation_id",
				"status");
	}

	@Test
	void duplicateRolesAreRejectedByTheMigratedSchema() throws Exception {
		try (Connection connection = dataSource.getConnection();
				var statement = connection.prepareStatement("""
						INSERT INTO sde_role (sde_role, description)
						VALUES ('User', 'duplicate role')
						""")) {
			assertThat(org.assertj.core.api.Assertions.catchThrowable(statement::executeUpdate))
					.hasMessageContaining("duplicate key");
		}
	}

	private List<String> existingTables() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metadata = connection.getMetaData();
			try (ResultSet resultSet = metadata.getTables(null, "public", "%", new String[] { "TABLE" })) {
				return readSingleColumn(resultSet, "TABLE_NAME");
			}
		}
	}

	private List<String> readSingleColumn(ResultSet resultSet) throws SQLException {
		return readSingleColumn(resultSet, "sde_permission");
	}

	private List<String> readSingleColumn(ResultSet resultSet, String columnName) throws SQLException {
		java.util.ArrayList<String> values = new java.util.ArrayList<>();
		while (resultSet.next()) {
			values.add(resultSet.getString(columnName));
		}
		return values;
	}

	private List<String> existingColumns(String tableName) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metadata = connection.getMetaData();
			try (ResultSet resultSet = metadata.getColumns(null, "public", tableName, "%")) {
				return readSingleColumn(resultSet, "COLUMN_NAME");
			}
		}
	}
}
