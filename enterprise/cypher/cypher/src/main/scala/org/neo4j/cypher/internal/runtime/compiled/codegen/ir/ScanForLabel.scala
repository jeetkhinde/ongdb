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
package org.neo4j.cypher.internal.runtime.compiled.codegen.ir

import org.neo4j.cypher.internal.runtime.compiled.codegen.spi.MethodStructure
import org.neo4j.cypher.internal.runtime.compiled.codegen.{CodeGenContext, Variable}

case class ScanForLabel(opName: String, labelName: String, labelVar: String) extends LoopDataGenerator {

  override def init[E](generator: MethodStructure[E])(implicit context: CodeGenContext) =
    generator.lookupLabelId(labelVar, labelName)

  override def produceLoopData[E](cursorName: String, generator: MethodStructure[E])(implicit context: CodeGenContext) = {
    generator.labelScan(cursorName, labelVar)
    generator.incrementDbHits()
  }

  override def getNext[E](nextVar: Variable, cursorName: String, generator: MethodStructure[E])
                         (implicit context: CodeGenContext) = {
    generator.incrementDbHits()
    generator.nodeFromNodeLabelIndexCursor(nextVar.name, cursorName)
  }

  override def checkNext[E](generator: MethodStructure[E], cursorName: String): E = generator.advanceNodeLabelIndexCursor(cursorName)

  override def close[E](cursorName: String, generator: MethodStructure[E]): Unit = generator.closeNodeLabelIndexCursor(cursorName)
}
