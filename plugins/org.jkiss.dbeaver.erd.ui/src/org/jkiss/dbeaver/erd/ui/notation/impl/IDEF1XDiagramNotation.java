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
package org.jkiss.dbeaver.erd.ui.notation.impl;

import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.erd.model.ERDUtils;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotation;
import org.jkiss.dbeaver.erd.ui.part.AssociationPart.CircleDecoration;
import org.jkiss.dbeaver.erd.ui.part.AssociationPart.RhombusDecoration;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

public class IDEF1XDiagramNotation implements ERDNotation {

    @Override
    public void applyNotation(PolylineConnection conn, ERDAssociation association, Color bckColor, Color frgColor) {

        boolean identifying = ERDUtils.isIdentifyingAssociation(association);
        DBSEntityConstraintType constraintType = association.getObject().getConstraintType();
        if (constraintType == DBSEntityConstraintType.INHERITANCE) {
            final PolygonDecoration srcDec = new PolygonDecoration();
            srcDec.setTemplate(PolygonDecoration.TRIANGLE_TIP);
            srcDec.setFill(true);
            srcDec.setBackgroundColor(bckColor);
            srcDec.setScale(10, 6);
            conn.setTargetDecoration(srcDec);
        } else if (constraintType.isAssociation() &&
            association.getSourceEntity() instanceof ERDEntity &&
            association.getTargetEntity() instanceof ERDEntity) {
            final CircleDecoration sourceDecor = new CircleDecoration();
            sourceDecor.setRadius(3);
            sourceDecor.setFill(true);
            sourceDecor.setBackgroundColor(frgColor);
            
            conn.setSourceDecoration(sourceDecor);
            if (ERDUtils.isOptionalAssociation(association)) {
                final RhombusDecoration targetDecor = new RhombusDecoration();
                targetDecor.setBackgroundColor(bckColor);
               
                conn.setTargetDecoration(targetDecor);
            }
        }

        conn.setLineWidth(2);
        if (!identifying || constraintType.isLogical()) {
            final DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();
            if (store.getString(ERDUIConstants.PREF_ROUTING_TYPE).equals(ERDUIConstants.ROUTING_MIKAMI)) {
                conn.setLineStyle(SWT.LINE_DOT);
            } else {
                conn.setLineStyle(SWT.LINE_CUSTOM);
            }
            conn.setLineDash(
                constraintType.isLogical() ? new float[] { 4 } : new float[] { 5 });
        }

//        final DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();
//        conn.setConnectionRouter(cLayer.getConnectionRouter());
//        if (!CommonUtils.isEmpty(association.getInitBends())) {
//            List<AbsoluteBendpoint> connBends = new ArrayList<>();
//            for (int[] bend : association.getInitBends()) {
//                connBends.add(new AbsoluteBendpoint(bend[0], bend[1]));
//            }
//            conn.setRoutingConstraint(connBends);
//        } else if (association.getTargetEntity() != null && association.getTargetEntity() == association.getSourceEntity()) {
//            EditPart entityPart = source;
//            if (entityPart == null) {
//                entityPart = target;
//            }
//            if (entityPart instanceof GraphicalEditPart
//                && (!store.getString(ERDUIConstants.PREF_ROUTING_TYPE).equals(ERDUIConstants.ROUTING_MIKAMI)
//                    || ERDAttributeVisibility.isHideAttributeAssociations(store))) {
//                final IFigure entityFigure = ((GraphicalEditPart) entityPart).getFigure();
//                final Dimension figureSize = entityFigure.getMinimumSize();
//                int entityWidth = figureSize.width;
//                int entityHeight = figureSize.height;
//                List<RelativeBendpoint> bends = new ArrayList<>();
//                RelativeBendpoint bpSource = new RelativeBendpoint(conn);
//                bpSource.setRelativeDimensions(new Dimension(entityWidth, entityHeight / 2),
//                    new Dimension(entityWidth / 2, entityHeight / 2));
//                bends.add(bpSource);
//                RelativeBendpoint bpTarget = new RelativeBendpoint(conn);
//                bpTarget.setRelativeDimensions(new Dimension(-entityWidth, entityHeight / 2), new Dimension(entityWidth, entityHeight));
//                bends.add(bpTarget);
//                conn.setRoutingConstraint(bends);
//            }
//        }
    }
}
