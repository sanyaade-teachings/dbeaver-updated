/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.stm;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ErrorNodeImpl;
import org.jkiss.code.NotNull;

import java.util.List;
import java.util.Set;


public class TreeTermErrorNode extends ErrorNodeImpl implements STMTreeNode {
    
    public TreeTermErrorNode(@NotNull Token symbol) {
        super(symbol);
    }
   
    private static String nodeName = "error"; 
    
    @Override
    public void fixup(ParserOverrides parserCtx) {
    }
    
    @Override
    public String getNodeName() {
        return nodeName;
    }
    
    @NotNull
    public Interval getRealInterval() {
        return new Interval(this.getSymbol().getStartIndex(), this.getSymbol().getStopIndex());
    }

}
