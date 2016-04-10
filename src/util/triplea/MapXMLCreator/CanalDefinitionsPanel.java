package util.triplea.MapXMLCreator;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import games.strategy.util.Tuple;
import util.triplea.MapXMLCreator.TerritoryDefinitionDialog.DEFINITION;


public class CanalDefinitionsPanel extends ImageScrollPanePanel {
  private static final String NEW_CANAL_OPTION = "<new Canal>";

  private SortedSet<String> selectedLandTerritories = new TreeSet<String>();
  private SortedSet<String> selectedWaterTerritories = new TreeSet<String>();
  private String currentCanalName = null;

  private CanalDefinitionsPanel() {}

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {
    ImageScrollPanePanel.mapXMLCreator = mapXMLCreator;
    final CanalDefinitionsPanel panel = new CanalDefinitionsPanel();
    panel.layout(stepActionPanel);
    mapXMLCreator.setAutoFillAction(new AbstractAction() {
      private static final long serialVersionUID = -8508734371454749752L;

      @Override
      public void actionPerformed(ActionEvent arg0) {
        final int prevCanalCount = MapXMLHelper.canalDefinitions.size();
        if (prevCanalCount > 0) {
          if (JOptionPane.YES_OPTION != JOptionPane.showOptionDialog(null,
              "All current canal definitions will be deleted.\rDo you want to continue with Auto-Fill?",
              "Auto-Fill Warning",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE, null, null, JOptionPane.NO_OPTION))
            return;
          MapXMLHelper.clearCanalDefinitions();
        }
        panel.clearSelection();
        final HashMap<String, LinkedHashSet<String>> landWaterTerritoyConnections =
            new HashMap<String, LinkedHashSet<String>>();
        final HashMap<String, LinkedHashSet<String>> waterLandTerritoyConnections =
            new HashMap<String, LinkedHashSet<String>>();
        for (final Entry<String, LinkedHashSet<String>> terrConn : MapXMLHelper.territoryConnections.entrySet()) {
          if (MapXMLHelper.territoryDefintions.get(terrConn.getKey()).get(DEFINITION.IS_WATER) == true) {
            LinkedHashSet<String> landTerrValue = new LinkedHashSet<String>();
            for (final String terr : terrConn.getValue()) {
              if (MapXMLHelper.territoryDefintions.get(terr).get(DEFINITION.IS_WATER) != true) {
                landTerrValue.add(terr);
              }
            }
            if (!landTerrValue.isEmpty())
              waterLandTerritoyConnections.put(terrConn.getKey(), landTerrValue);
          } else {
            LinkedHashSet<String> waterTerrValue = new LinkedHashSet<String>(terrConn.getValue());
            for (Iterator<String> iterator = waterTerrValue.iterator(); iterator.hasNext();) {
              if (MapXMLHelper.territoryDefintions.get(iterator.next()).get(DEFINITION.IS_WATER) != true) {
                iterator.remove();
              }
            }
            landWaterTerritoyConnections.put(terrConn.getKey(), waterTerrValue);
          }
        }
        for (final Entry<String, LinkedHashSet<String>> terrConn : waterLandTerritoyConnections.entrySet()) {
          final String waterTerr = terrConn.getKey();
          for (final String landTerr : terrConn.getValue()) {
            HashSet<String> waterTerrs = landWaterTerritoyConnections.get(landTerr);
            if (waterTerrs == null) {
              final LinkedHashSet<String> newWaterTerrs = new LinkedHashSet<String>();
              newWaterTerrs.add(waterTerr);
              landWaterTerritoyConnections.put(landTerr, newWaterTerrs);
            } else
              waterTerrs.add(waterTerr);
          }

        }
        final HashMap<String, LinkedHashSet<String>> landWaterTerrConnChecks =
            new HashMap<String, LinkedHashSet<String>>(landWaterTerritoyConnections);
        for (final Entry<String, LinkedHashSet<String>> landWaterTerrConn : landWaterTerritoyConnections.entrySet()) {
          final String landTerr = landWaterTerrConn.getKey();
          final LinkedHashSet<String> waterTerrs = landWaterTerrConn.getValue();
          LinkedHashSet<String> landTerrNeighbors = MapXMLHelper.territoryConnections.get(landTerr);
          if (landTerrNeighbors == null)
            landTerrNeighbors = new LinkedHashSet<String>();
          landWaterTerrConnChecks.remove(landTerr);
          for (final Entry<String, LinkedHashSet<String>> landWaterTerrConn2 : landWaterTerrConnChecks.entrySet()) {
            final String landTerr2 = landWaterTerrConn2.getKey();
            // if (landTerrNeighbors.contains(landTerr2))
            // continue;
            LinkedHashSet<String> landTerrNeighbors2 = MapXMLHelper.territoryConnections.get(landTerr2);
            if (landTerrNeighbors2 == null)
              landTerrNeighbors2 = new LinkedHashSet<String>();
            // if (landTerrNeighbors2.contains(landTerr))
            // continue;
            if (!landTerrNeighbors.contains(landTerr2) && !landTerrNeighbors2.contains(landTerr))
              continue;
            final SortedSet<String> waterTerrs2 = new TreeSet<String>(landWaterTerrConn2.getValue());
            waterTerrs2.retainAll(waterTerrs);
            if (waterTerrs2.size() > 1) {
              // remove water territories that are not connected to the other water territories
              final SortedSet<String> waterTerrs2Copy = new TreeSet<String>(waterTerrs2);
              for (Iterator<String> iter_waterTerr2 = waterTerrs2.iterator(); iter_waterTerr2.hasNext();) {
                final String waterTerr2 = (String) iter_waterTerr2.next();
                waterTerrs2Copy.remove(waterTerr2);
                final SortedSet<String> waterTerrs2ReqNeighbors = new TreeSet<String>(waterTerrs2Copy);
                final LinkedHashSet<String> waterTerr2Neightbors = MapXMLHelper.territoryConnections.get(waterTerr2);
                if (waterTerr2Neightbors != null) {
                  for (final String waterTerr2Neighbor : waterTerr2Neightbors)
                    waterTerrs2ReqNeighbors.remove(waterTerr2Neighbor);
                }
                if (!waterTerrs2ReqNeighbors.isEmpty())
                  iter_waterTerr2.remove();
              }
              // create canal only if at least 2 water territories remain
              if (waterTerrs2.size() > 1) {
                final SortedSet<String> newLandSet = new TreeSet<String>();
                newLandSet.add(landTerr);
                newLandSet.add(landTerr2);
                final Tuple<SortedSet<String>, SortedSet<String>> terrTuple =
                    Tuple.of(new TreeSet<String>(waterTerrs2), newLandSet);
                MapXMLHelper.putCanalDefinitions("Canal" + MapXMLHelper.canalDefinitions.size(), terrTuple);
              }
            }
          }
        }
        boolean noNewCanalsBuild = MapXMLHelper.canalDefinitions.isEmpty();
        if (noNewCanalsBuild)
          JOptionPane.showMessageDialog(null, "No canals have been build!", "Auto-Fill Result",
              JOptionPane.PLAIN_MESSAGE);
        else {
          final StringBuilder sb = new StringBuilder();
          sb.append("<html>The following " + MapXMLHelper.canalDefinitions.size() + " canals have been build:");
          for (final Entry<String, Tuple<SortedSet<String>, SortedSet<String>>> canalDef : MapXMLHelper.canalDefinitions
              .entrySet()) {
            sb.append("<br/> - " + canalDef.getKey() + ": ");
            Iterator<String> iter_waterTerr = canalDef.getValue().getFirst().iterator();
            sb.append(iter_waterTerr.next());
            while (iter_waterTerr.hasNext())
              sb.append("-" + iter_waterTerr.next());
          }
          sb.append("</html>");
          JOptionPane.showMessageDialog(null, sb.toString(), "Auto-Fill Result", JOptionPane.PLAIN_MESSAGE);
        }
        if (prevCanalCount > 0 || !noNewCanalsBuild)
          panel.repaint();
      }
    });
  }

  protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics,
      final Point item, final int x_text_start) {
    if (selectedLandTerritories.contains(centerName) || selectedWaterTerritories.contains(centerName)) {
      final Rectangle2D stringBounds = fontMetrics.getStringBounds(centerName, g);
      g.setColor(Color.yellow);
      g.fillRect(Math.max(0, x_text_start - 2), Math.max(0, item.y - 6), (int) stringBounds.getWidth() + 4,
          (int) stringBounds.getHeight());
      g.setColor(Color.red);
      g.drawString(centerName, Math.max(0, x_text_start), item.y + 5);
    }
    g.setColor(Color.red);
  }

  protected void paintPreparation(final Map<String, Point> centers) {}

  @Override
  protected void paintOwnSpecifics(Graphics g, Map<String, Point> centers) {
    Graphics2D g2d = (Graphics2D) g;
    for (final Entry<String, Tuple<SortedSet<String>, SortedSet<String>>> canalDef : MapXMLHelper.canalDefinitions
        .entrySet()) {
      // Water Territories
      g.setColor(Color.BLUE);
      final String canalName = canalDef.getKey();
      final FontMetrics fontMetrics = g.getFontMetrics();
      SortedSet<String> terrSet1 = canalDef.getValue().getFirst();
      SortedSet<String> remainingTerrs = new TreeSet<String>(terrSet1);
      for (final String terr1 : terrSet1) {
        final Point center1 = centers.get(terr1);
        remainingTerrs.remove(terr1);
        for (final String terr2 : remainingTerrs) {
          final Point center2 = centers.get(terr2);
          g.drawLine(center1.x, center1.y, center2.x, center2.y);
          Rectangle2D stringBounds = fontMetrics.getStringBounds(canalName, g);
          int dX = center2.x - center1.x;
          int dY = center2.y - center1.y;
          final Point lineCenter = new Point(center1.x + dX / 2, center1.y + dY / 2);
          double centerDistance = center2.distance(center1);
          if (centerDistance > stringBounds.getWidth())
            drawRotate(g2d, lineCenter.x, lineCenter.y, Math.atan2(dY, dX), canalName,
                (int) (stringBounds.getWidth()) / -2);
          else
            g.drawString(canalName, lineCenter.x, lineCenter.y);
        }
      }
      // Land Territories
      g.setColor(Color.GREEN);
      terrSet1 = canalDef.getValue().getSecond();
      remainingTerrs = new TreeSet<String>(terrSet1);
      for (final String terr1 : terrSet1) {
        final Point center1 = centers.get(terr1);
        remainingTerrs.remove(terr1);
        for (final String terr2 : remainingTerrs) {
          final Point center2 = centers.get(terr2);
          g.drawLine(center1.x, center1.y, center2.x, center2.y);
        }
      }
    }

  }

  static final double piHalf = Math.PI / 2;

  public static void drawRotate(final Graphics2D g2d, final double x, final double y, double radianAngle,
      final String text, final int xOffset) {
    g2d.translate((float) x, (float) y);
    if (radianAngle > piHalf)
      radianAngle -= Math.PI;
    else if (radianAngle < -piHalf)
      radianAngle += Math.PI;
    g2d.rotate(radianAngle);
    g2d.drawString(text, xOffset, -2);
    g2d.rotate(-radianAngle);
    g2d.translate(-(float) x, -(float) y);
  }

  protected void mouseClickedOnImage(final Map<String, Point> centers, final JPanel imagePanel, final MouseEvent e) {
    if (SwingUtilities.isRightMouseButton(e)) {
      if (currentCanalName != null
          && (selectedLandTerritories.size() < 2 || selectedWaterTerritories.size() < 2)) {
        if (JOptionPane.YES_OPTION != JOptionPane.showOptionDialog(null, "Canal '" + currentCanalName
            + "' is incomplete. A canal needs at least 2 land and 2 water territories.\rDo you want to continue to deselect the canal?",
            "Canal incomplete",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, JOptionPane.NO_OPTION))
          return;
        MapXMLHelper.canalDefinitions.remove(currentCanalName);
      }
      currentCanalName = null;
      if (!selectedLandTerritories.isEmpty() || !selectedWaterTerritories.isEmpty()) {
        clearSelection();
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            imagePanel.repaint();
          }
        });
      }
      return;
    }

    final Point point = e.getPoint();
    final String newTerrName = findTerritoryName(point, polygons);

    if (newTerrName == null)
      return;
    Boolean newTerrIsWater = MapXMLHelper.territoryDefintions.get(newTerrName).get(DEFINITION.IS_WATER);
    if (newTerrIsWater == null)
      newTerrIsWater = false;

    final LinkedHashSet<String> newTerrNeighborsDiffType = getNeighborsByType(newTerrName, !newTerrIsWater);

    final ArrayList<String> terrCanals = new ArrayList<String>();
    if (newTerrIsWater) {
      for (final Entry<String, Tuple<SortedSet<String>, SortedSet<String>>> canalDef : MapXMLHelper.canalDefinitions
          .entrySet()) {
        if (canalDef.getValue().getFirst().contains(newTerrName))
          terrCanals.add(canalDef.getKey());
      }
    } else {
      for (final Entry<String, Tuple<SortedSet<String>, SortedSet<String>>> canalDef : MapXMLHelper.canalDefinitions
          .entrySet()) {
        if (canalDef.getValue().getSecond().contains(newTerrName))
          terrCanals.add(canalDef.getKey());
      }
    }

    if (currentCanalName == null) {
      if (newTerrNeighborsDiffType.size() < 2) {
        JOptionPane.showMessageDialog(null, "The selected " + (newTerrIsWater ? "water" : "land")
            + " territory is connected to less than 2 " + (!newTerrIsWater ? "water" : "land")
            + " territories!", "Input Error", JOptionPane.ERROR_MESSAGE);
        return;
      }
      if (!terrCanals.isEmpty()) {
        terrCanals.add(NEW_CANAL_OPTION);
        currentCanalName = (String) JOptionPane.showInputDialog(null,
            "Which canal should be selected for territory '" + newTerrName + "?", "Choose Canal",
            JOptionPane.QUESTION_MESSAGE,
            null, terrCanals.toArray(new String[terrCanals.size()]), // Array of choices
            terrCanals.get(0)); // Initial choice
      }
      if (terrCanals.isEmpty() || NEW_CANAL_OPTION.equals(currentCanalName)) {
        String suggestedCanalName;
        int counter = MapXMLHelper.canalDefinitions.size();
        do {
          suggestedCanalName = "Canal" + counter;
          ++counter;
        } while (MapXMLHelper.canalDefinitions.keySet().contains(currentCanalName));
        currentCanalName = JOptionPane.showInputDialog(null,
            "Which canal should be selected for territory '" + newTerrName + "?", suggestedCanalName);

        while (MapXMLHelper.canalDefinitions.keySet().contains(currentCanalName)) {
          JOptionPane.showMessageDialog(null, "The canal name " + currentCanalName + " is already in use!",
              "Input Error", JOptionPane.ERROR_MESSAGE);
          currentCanalName = JOptionPane.showInputDialog(null,
              "Which canal should be selected for territory '" + newTerrName + "?", currentCanalName);
        }
      } else if (currentCanalName != null) {
        final Tuple<SortedSet<String>, SortedSet<String>> canalTerrs =
            MapXMLHelper.canalDefinitions.get(currentCanalName);
        selectedWaterTerritories = canalTerrs.getFirst();
        selectedLandTerritories = canalTerrs.getSecond();
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            imagePanel.repaint();
          }
        });
        return;
      }
      if (currentCanalName == null)
        return;
    }

    if (currentCanalName != null) {
      Tuple<SortedSet<String>, SortedSet<String>> canalTerrs = MapXMLHelper.canalDefinitions.get(currentCanalName);
      if (canalTerrs == null) {
        canalTerrs = Tuple.of(new TreeSet<String>(), new TreeSet<String>());
        MapXMLHelper.putCanalDefinitions(currentCanalName, canalTerrs);
      }

      selectedWaterTerritories = canalTerrs.getFirst();
      selectedLandTerritories = canalTerrs.getSecond();

      final SortedSet<String> selectedTerrsSameType;
      if (newTerrIsWater)
        selectedTerrsSameType = selectedWaterTerritories;
      else
        selectedTerrsSameType = selectedLandTerritories;

      if (selectedTerrsSameType.size() > 0) {
        final SortedSet<String> commonNeighborsDiffType =
            getCommonNeighborsOfType(selectedTerrsSameType, !newTerrIsWater);
        commonNeighborsDiffType.retainAll(newTerrNeighborsDiffType);

        if (commonNeighborsDiffType.size() < 2) {
          JOptionPane.showMessageDialog(null, "The selected " + (newTerrIsWater ? "water" : "land")
              + " territory is connected to less than 2 common " + (!newTerrIsWater ? "water" : "land")
              + " territories with the other " + (newTerrIsWater ? "water" : "land") + " territor"
              + (selectedTerrsSameType.size() == 1 ? "y" : "ies")
              + "!\rRight click to deselect current canal '" + currentCanalName + "'.", "Input Error",
              JOptionPane.ERROR_MESSAGE);
          return;
        }
      }

      if (selectedTerrsSameType.contains(newTerrName))
        selectedTerrsSameType.remove(newTerrName);
      else
        selectedTerrsSameType.add(newTerrName);

    }

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        imagePanel.repaint();
      }
    });
  }

  protected void clearSelection() {
    currentCanalName = null;
    selectedLandTerritories = new TreeSet<String>();
    selectedWaterTerritories = new TreeSet<String>();
  }

  protected LinkedHashSet<String> getNeighborsByType(final String newTerrName, final boolean waterNeighbors) {
    final LinkedHashSet<String> neighborsByType = new LinkedHashSet<String>();
    final LinkedHashSet<String> neighbors = MapXMLHelper.territoryConnections.get(newTerrName);
    for (final Entry<String, LinkedHashSet<String>> terrConnEntry : MapXMLHelper.territoryConnections.entrySet()) {
      if (MapXMLHelper.territoryDefintions.get(terrConnEntry.getKey()).get(DEFINITION.IS_WATER) == waterNeighbors
          && terrConnEntry.getValue().contains(newTerrName))
        neighborsByType.add(terrConnEntry.getKey());
    }
    if (neighbors != null) {
      for (final String neighbor : neighbors) {
        if (MapXMLHelper.territoryDefintions.get(neighbor).get(DEFINITION.IS_WATER) == waterNeighbors)
          neighborsByType.add(neighbor);
      }
    }
    return neighborsByType;
  }

  protected SortedSet<String> getCommonNeighborsOfType(final SortedSet<String> terrList, final boolean typeIsWater) {
    final SortedSet<String> commonNeighborsOfType = new TreeSet<String>();
    final HashMap<String, Collection<String>> neighborsMap = new HashMap<String, Collection<String>>();
    for (final String terr : terrList)
      neighborsMap.put(terr, new ArrayList<String>());
    for (final Entry<String, LinkedHashSet<String>> terrConnEntry : MapXMLHelper.territoryConnections.entrySet()) {
      final String terr1 = terrConnEntry.getKey();
      if (terrList.contains(terr1)) {
        for (final String terr2 : terrConnEntry.getValue()) {
          if (MapXMLHelper.territoryDefintions.get(terr2).get(DEFINITION.IS_WATER) == typeIsWater)
            neighborsMap.get(terr1).add(terr2);
        }
      } else {
        if (MapXMLHelper.territoryDefintions.get(terr1).get(DEFINITION.IS_WATER) == typeIsWater) {
          SortedSet<String> selectedTerritoriesCopy = new TreeSet<String>(terrList);
          selectedTerritoriesCopy.retainAll(terrConnEntry.getValue());
          for (final String terr2 : selectedTerritoriesCopy) {
            neighborsMap.get(terr2).add(terr1);
          }
        }
      }
    }
    commonNeighborsOfType.addAll(neighborsMap.values().iterator().next());
    if (commonNeighborsOfType.size() >= 2) {
      for (final Collection<String> waterNeighors : neighborsMap.values()) {
        commonNeighborsOfType.retainAll(waterNeighors);
        if (commonNeighborsOfType.size() < 2)
          break;
      }
    }
    return commonNeighborsOfType;
  }
}