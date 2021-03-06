package com.prisma.api.mutations.nonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ApiConnectorCapability.JoinRelationsCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NonEmbeddedSettingNodeSelectorToNullSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationsCapability)

  "Setting a where value to null " should "should only update one if there are several nulls for the specified node selector" in {
    val project = SchemaDsl.fromString() {
      """
        |type A {
        |  id: ID! @unique
        |  b: String @unique
        |  key: String! @unique
        |  c: C
        |}
        |
        |type C {
        |  id: ID! @unique
        |  c: String
        |}
      """
    }
    database.setup(project)

    server.query(
      """mutation a {
        |  createA(data: {
        |    b: "abc"
        |    key: "abc"
        |    c: {
        |       create:{ c: "C"}
        |    }
        |  }) {
        |    id
        |    key,
        |    b,
        |    c {c}
        |  }
        |}""",
      project
    )

    server.query(
      """mutation a {
        |  createA(data: {
        |    b: null
        |    key: "abc2"
        |    c: {
        |       create:{ c: "C2"}
        |    }
        |  }) {
        |    key,
        |    b,
        |    c {c}
        |  }
        |}""",
      project
    )

    server.query(
      """mutation b {
        |  updateA(
        |    where: { b: "abc" }
        |    data: {
        |      b: null
        |      c: {update:{c:"NewC"}}
        |    }) {
        |    b
        |    c{c}
        |  }
        |}""",
      project
    )

    val result = server.query(
      """
        |{
        | as {
        |   b
        |   c {
        |     c
        |   }
        | }
        |}
      """.stripMargin,
      project
    )

    result.toString should be("""{"data":{"as":[{"b":null,"c":{"c":"NewC"}},{"b":null,"c":{"c":"C2"}}]}}""")
  }

}
