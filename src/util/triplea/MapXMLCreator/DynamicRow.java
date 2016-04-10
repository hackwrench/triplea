package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Base class for the other *Row classes defining one removable input row.
 * Its functionality is highly interlinked DynamicRowsPanel.
 * 
 * @see DynamicRowsPanel
 * @author Erik von der Osten
 * 
 */
public abstract class DynamicRow {

  final static int INPUT_FIELD_SIZE_LARGE = 150;
  final static int INPUT_FIELD_SIZE_MEDIUM = 120;
  final static int INPUT_FIELD_SIZE_SMALL = 55;

  private DynamicRowsPanel parentRowPanel;
  protected String currentRowName;
  protected JButton bRemoveRow;

  protected DynamicRow(final String rowName, final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel) {
    currentRowName = rowName;
    this.parentRowPanel = parentRowPanel;

    bRemoveRow = new JButton("X");
    bRemoveRow.setFont(new Font("Tahoma", Font.PLAIN, 11));
    final Dimension dimension = bRemoveRow.getPreferredSize();
    dimension.width = 25;
    bRemoveRow.setPreferredSize(dimension);
    bRemoveRow.addActionListener(new AbstractAction("Remove Row") {
      private static final long serialVersionUID = 7725213146244928366L;

      public void actionPerformed(final ActionEvent e) {
        removeRowAction();

        pushUpRowsTo(currentRowName);

        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            stepActionPanel.revalidate();
            stepActionPanel.repaint();
          }
        });
      }
    });
  }

  public void addToComponent(final JComponent parent, final int rowIndex) {
    final GridBagConstraints gbc_template = new GridBagConstraints();
    gbc_template.insets = new Insets(0, 0, 5, 5);
    gbc_template.gridy = rowIndex;
    gbc_template.gridx = 0;
    gbc_template.anchor = GridBagConstraints.WEST;
    addToComponent(parent, rowIndex, gbc_template);
  }

  public void addToComponent(final JComponent parent, final int rowIndex, final GridBagConstraints gbc_template) {
    gbc_template.gridy = rowIndex;
    addToComponent(parent, gbc_template);
  }

  private void adaptRow(final DynamicRow newRow) {
    this.currentRowName = newRow.currentRowName;
    adaptRowSpecifics(newRow);
  }

  private void pushUpRowsTo(final String currentRowName) {
    // go to currentRowName row, update below rows and delete last row
    final LinkedHashSet<DynamicRow> rows = parentRowPanel.getRows();
    final Iterator<DynamicRow> iter_rows = rows.iterator();
    if (iter_rows.hasNext()) {
      DynamicRow curr_row = iter_rows.next();
      while (iter_rows.hasNext() && !currentRowName.equals(curr_row.getRowName())) {
        curr_row = iter_rows.next();
      }
      while (iter_rows.hasNext()) {
        final DynamicRow next_row = iter_rows.next();
        curr_row.adaptRow(next_row);
        curr_row = next_row;
      }
      curr_row.removeFromStepPanel();
      iter_rows.remove();
    }

    DynamicRowsPanel parentPanel = ((DynamicRowsPanel) parentRowPanel);
    parentPanel.removeFinalButtonRow();

    GridBagConstraints gbc_templateButton = new GridBagConstraints();
    gbc_templateButton.insets = new Insets(0, 0, 5, 5);
    gbc_templateButton.gridy = rows.size() + 1;
    gbc_templateButton.gridx = 0;
    gbc_templateButton.anchor = GridBagConstraints.WEST;
    parentPanel.addFinalButtonRow(gbc_templateButton);
  }

  private void removeFromStepPanel() {
    final ArrayList<JComponent> componentList = getComponentList();
    componentList.add(bRemoveRow);

    ((DynamicRowsPanel) parentRowPanel).removeComponents(componentList);
  }

  public String getRowName() {
    return currentRowName;
  }

  @Override
  public String toString() {
    return currentRowName;
  }

  abstract protected ArrayList<JComponent> getComponentList();

  abstract public void addToComponent(final JComponent parent, final GridBagConstraints gbc_template);

  abstract protected void adaptRowSpecifics(final DynamicRow newRow);

  abstract protected void removeRowAction();
}