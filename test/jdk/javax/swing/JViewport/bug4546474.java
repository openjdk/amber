/*
 * Copyright (c) 2003, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/* @test
 * @bug 4546474
 * @summary JScrollPane's always-visible scrollbars not updated when
 * viewport is replaced
 * @run main bug4546474
 */

import java.awt.Dimension;
import java.awt.Robot;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class bug4546474 {
    static JScrollPane scrollpane;
    static JScrollBar sbar;
    static volatile boolean viewChanged;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = new JPanel();
            panel.setPreferredSize(new Dimension(500, 500));
            scrollpane = new JScrollPane(panel,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sbar = scrollpane.getVerticalScrollBar();
        });

        Robot robot = new Robot();
        robot.delay(500);
        SwingUtilities.invokeAndWait(() -> {
            sbar.addAdjustmentListener(e -> viewChanged = true);
            scrollpane.setViewportView(null);
        });
        robot.delay(500);
        if (!viewChanged) {
            viewChanged = true;
        }
        robot.delay(500);

        SwingUtilities.invokeAndWait(() -> {
            if (sbar.getVisibleAmount() > 0) {
                throw new RuntimeException("Vertical scrollbar is not " +
                        "updated when viewport is replaced");
            }
        });
    }
}