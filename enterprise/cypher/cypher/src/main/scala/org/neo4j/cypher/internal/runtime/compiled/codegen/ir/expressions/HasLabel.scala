/*
 * Copyright (c) 2018-2020 "Graph Foundation"
 * Graph Foundation, Inc. [https://graphfoundation.org]
 *
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of ONgDB Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) as found
 * in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 */
package org.neo4j.cypher.internal.runtime.compiled.codegen.ir.expressions

import org.neo4j.cypher.internal.runtime.compiled.codegen.spi.MethodStructure
import org.neo4j.cypher.internal.runtime.compiled.codegen.{CodeGenContext, Variable}
import org.neo4j.cypher.internal.v3_6.util.symbols._

case class HasLabel(nodeVariable: Variable, labelVariable: String, labelName: String)
  extends CodeGenExpression {

  def init[E](generator: MethodStructure[E])(implicit context: CodeGenContext) =
    generator.lookupLabelId(labelVariable, labelName)

  def generateExpression[E](structure: MethodStructure[E])(implicit context: CodeGenContext) = {
    val localName = context.namer.newVarName()
    structure.declarePredicate(localName)

    structure.incrementDbHits()
    if (nodeVariable.nullable)
      structure.nullableReference(nodeVariable.name, CodeGenType.primitiveNode,
                                  structure.box(
                                    structure.hasLabel(nodeVariable.name, labelVariable, localName), CodeGenType.primitiveBool))
    else
      structure.hasLabel(nodeVariable.name, labelVariable, localName)
  }

  override def nullable(implicit context: CodeGenContext) = nodeVariable.nullable

  override def codeGenType(implicit context: CodeGenContext) =
    if (nullable) CypherCodeGenType(CTBoolean, ReferenceType) else CodeGenType.primitiveBool
}
