package io.fekav.req.shared.kg;

import io.quarkus.runtime.StartupEvent;
  import jakarta.enterprise.context.ApplicationScoped;
  import jakarta.enterprise.event.Observes;
  import jakarta.inject.Inject;
  import org.neo4j.driver.Driver;
  import org.neo4j.driver.Session;
  import org.neo4j.driver.Values;

  @ApplicationScoped
  public class Neo4jSchemaInitializer {

      private final Driver driver;

      @Inject
      Neo4jSchemaInitializer(Driver driver) {
          this.driver = driver;
      }

      void onStart(@Observes StartupEvent event) {
          try (Session session = driver.session()) {
              session.run("""
                  CREATE CONSTRAINT requirement_id IF NOT EXISTS
                  FOR (r:Requirement) REQUIRE r.id IS UNIQUE
                  """).consume();

              session.run("""
                  CREATE CONSTRAINT requirement_type_code IF NOT EXISTS
                  FOR (t:RequirementType) REQUIRE t.code IS UNIQUE
                  """).consume();

              session.run("""
                  CREATE CONSTRAINT requirement_property_code IF NOT EXISTS
                  FOR (p:RequirementProperty) REQUIRE p.code IS UNIQUE
                  """).consume();

              session.run("""
                  CREATE CONSTRAINT syntax_role_code IF NOT EXISTS
                  FOR (r:SyntaxRole) REQUIRE r.code IS UNIQUE
                  """).consume();

              session.run("""
                  CREATE CONSTRAINT syntax_element_id IF NOT EXISTS
                  FOR (e:SyntaxElement) REQUIRE e.id IS UNIQUE
                  """).consume();

              session.executeWrite(tx -> {
                  tx.run("""
                      UNWIND $codes AS code
                      MERGE (:RequirementType {code: code})
                      """, Values.parameters("codes", java.util.List.of("GOAL", "NEED",
                      "REQUIREMENT")));

                  tx.run("""
                      UNWIND $codes AS code
                      MERGE (:RequirementProperty {code: code})
                      """, Values.parameters("codes", java.util.List.of("FUNCTIONAL", "QUALITY")));

                  tx.run("""
                      UNWIND $codes AS code
                      MERGE (:SyntaxRole {code: code})
                      """, Values.parameters("codes", java.util.List.of(
                          "SUBJECT", "ACTION", "OBJECT", "CONDITION", "CONSTRAINT"
                      )));

                  return null;
              });
          }
      }
  }