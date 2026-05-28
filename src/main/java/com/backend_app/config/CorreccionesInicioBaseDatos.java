package com.backend_app.config;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@Profile("dev")
public class CorreccionesInicioBaseDatos implements ApplicationRunner {
	private static final Logger log = LoggerFactory.getLogger(CorreccionesInicioBaseDatos.class);

	private final JdbcTemplate jdbcTemplate;
	private final DataSource dataSource;

	public CorreccionesInicioBaseDatos(JdbcTemplate jdbcTemplate, DataSource dataSource) {
		this.jdbcTemplate = jdbcTemplate;
		this.dataSource = dataSource;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (!esPostgres()) return;
		log.warn("CorreccionesInicioBaseDatos activo (PostgreSQL): verificando columnas mal tipadas");
		log.warn("current_schema={} search_path={}", schemaActual(), jdbcTemplate.queryForObject("show search_path", String.class));
		corregirColumnasBytea();
	}

	private void corregirColumnasBytea() {
		corregirColumnasByteaEnTodosLosSchemas("products", List.of(
				new CorreccionColumna("name", "varchar(140)"),
				new CorreccionColumna("code", "varchar(60)"),
				new CorreccionColumna("description", "varchar(1000)"),
				new CorreccionColumna("image_url", "varchar(500)")
		));

		corregirColumnasByteaEnTodosLosSchemas("categories", List.of(
				new CorreccionColumna("name", "varchar(120)"),
				new CorreccionColumna("description", "varchar(500)")
		));
	}

	private void corregirColumnasByteaEnTodosLosSchemas(String tableName, List<CorreccionColumna> fixes) {
		List<String> schemas = schemasParaTabla(tableName);
		if (schemas.isEmpty()) return;
		log.warn("Tablas encontradas para {} en schemas={}", tableName, schemas);

		for (String schema : schemas) {
			for (CorreccionColumna fix : fixes) {
				TipoColumna columnType = tipoColumna(schema, tableName, fix.nombreColumna());
				if (columnType == null) continue;
				if (!columnType.esBytea()) continue;

				String sql = "alter table " + q(schema) + "." + q(tableName) + " alter column " + q(fix.nombreColumna())
						+ " type " + fix.tipoObjetivo()
						+ " using convert_from(" + q(fix.nombreColumna()) + ", 'UTF8')";
				try {
					log.warn("Corrigiendo columna BYTEA: {}.{}.{} -> {}", schema, tableName, fix.nombreColumna(), fix.tipoObjetivo());
					jdbcTemplate.execute(sql);
				} catch (RuntimeException ex) {
					throw new IllegalStateException("No se pudo corregir el tipo BYTEA en " + schema + "." + tableName + "."
							+ fix.nombreColumna(), ex);
				}
			}
		}
	}

	private List<String> schemasParaTabla(String tableName) {
		return jdbcTemplate.query("""
				select table_schema
				from information_schema.tables
				where table_name = ?
				order by case when table_schema = current_schema() then 0 else 1 end, table_schema asc
				""", (rs, rowNum) -> rs.getString(1), tableName);
	}

	private TipoColumna tipoColumna(String schema, String tableName, String columnName) {
		return jdbcTemplate.query("""
				select data_type, udt_name
				from information_schema.columns
				where table_schema = ?
				  and table_name = ?
				  and column_name = ?
				""", rs -> rs.next() ? new TipoColumna(rs.getString(1), rs.getString(2)) : null, schema, tableName, columnName);
	}

	private boolean esPostgres() throws SQLException {
		try (var conn = dataSource.getConnection()) {
			DatabaseMetaData meta = conn.getMetaData();
			String name = meta.getDatabaseProductName();
			return name != null && name.toLowerCase(Locale.ROOT).contains("postgres");
		}
	}

	private String schemaActual() {
		return jdbcTemplate.queryForObject("select current_schema()", String.class);
	}

	private static String q(String identifier) {
		String safe = Objects.requireNonNull(identifier, "identifier");
		return "\"" + safe.replace("\"", "\"\"") + "\"";
	}

	private record CorreccionColumna(String nombreColumna, String tipoObjetivo) {
	}

	private record TipoColumna(String dataType, String udtName) {
		boolean esBytea() {
			return "bytea".equalsIgnoreCase(dataType) || "bytea".equalsIgnoreCase(udtName);
		}
	}
}
