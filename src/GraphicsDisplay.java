
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JPanel;

import static java.lang.Math.abs;

public class GraphicsDisplay extends JPanel {
    private ArrayList<Double[]> graphicsData;
    private ArrayList<Double[]> originalData;
    private int selectedMarker = -1;
    private boolean showAxis = true;
    private boolean showMarkers = true;
    private boolean showNullMarkers = false;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double[][] viewport = new double[2][2];
    //    private ArrayList<double[][]> undoHistory = new ArrayList(5);
    private double scaleX;
    private double scaleY;
    private BasicStroke axisStroke;
    private BasicStroke gridStroke;
    private BasicStroke markerStroke;
    private BasicStroke selectionStroke;
    private BasicStroke graphicsStroke;
    private Font axisFont;
    private Font labelsFont;
    private static DecimalFormat formatter = (DecimalFormat)NumberFormat.getInstance();
    private boolean scaleMode = false;
    private boolean changeMode = false;
    private double[] originalPoint = new double[2];
    private Rectangle2D.Double selectionRect = new Rectangle2D.Double();

    public GraphicsDisplay() {
        this.setBackground(Color.WHITE);
        graphicsStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 10.0f, new float[] {32, 16, 16, 32, 8}, 0.0f);
        this.axisStroke = new BasicStroke(2.0F, 0, 0, 10.0F, (float[])null, 0.0F);
        this.gridStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[]{4.0F, 4.0F}, 0.0F);
        this.markerStroke = new BasicStroke(1.0F, 0, 0, 10.0F, (float[])null, 0.0F);
        this.selectionStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[]{10.0F, 10.0F}, 0.0F);
        this.axisFont = new Font("Serif", 1, 36);
        this.labelsFont = new Font("Serif", 0, 10);
        formatter.setMaximumFractionDigits(3);
        this.addMouseListener(new MouseHandler());
        this.addMouseMotionListener(new MouseMotionHandler());
    }

    public void displayGraphics(ArrayList<Double[]> graphicsData) {
        this.graphicsData = graphicsData;
        this.originalData = new ArrayList(graphicsData.size());
        Iterator var3 = graphicsData.iterator();

        while(var3.hasNext()) {
            Double[] point = (Double[])var3.next();
            Double[] newPoint = new Double[]{new Double(point[0]), new Double(point[1])};
            this.originalData.add(newPoint);
        }

        this.minX = ((Double[])graphicsData.get(0))[0];
        this.maxX = ((Double[])graphicsData.get(graphicsData.size() - 1))[0];
        this.minY = ((Double[])graphicsData.get(0))[1];
        this.maxY = this.minY;

        for(int i = 1; i < graphicsData.size(); ++i) {
            if (((Double[])graphicsData.get(i))[1] < this.minY) {
                this.minY = ((Double[])graphicsData.get(i))[1];
            }

            if (((Double[])graphicsData.get(i))[1] > this.maxY) {
                this.maxY = ((Double[])graphicsData.get(i))[1];
            }
        }

        this.zoomToRegion(this.minX, this.maxY, this.maxX, this.minY);

    }

    public void zoomToRegion(double x1, double y1, double x2, double y2) {
        this.viewport[0][0] = x1;
        this.viewport[0][1] = y1;
        this.viewport[1][0] = x2;
        this.viewport[1][1] = y2;
        this.repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.scaleX = this.getSize().getWidth() / (this.viewport[1][0] - this.viewport[0][0]);
        this.scaleY = this.getSize().getHeight() / (this.viewport[0][1] - this.viewport[1][1]);
        if (this.graphicsData != null && this.graphicsData.size() != 0) {
            Graphics2D canvas = (Graphics2D)g;
            this.paintGrid(canvas);
            if (showAxis) paintAxis(canvas);
            this.paintGraphics(canvas);
            if (showMarkers) paintMarkers(canvas);
            this.paintLabels(canvas);
            this.paintSelection(canvas);
            if (showNullMarkers) paintNullMarkers(canvas);
        }
    }

    private void paintSelection(Graphics2D canvas) {
        if (this.scaleMode) {
            canvas.setStroke(this.selectionStroke);
            canvas.setColor(Color.BLACK);
            canvas.draw(this.selectionRect);
        }
    }

    private void paintGraphics(Graphics2D canvas) {
        canvas.setStroke(this.graphicsStroke);
        canvas.setColor(Color.RED);
        Double currentX = null;
        Double currentY = null;
        Iterator var5 = this.graphicsData.iterator();
        GeneralPath graphics = new GeneralPath();
        boolean flag = true;
        while (var5.hasNext()) {
            Double[] point = (Double[]) var5.next();
            if (!(point[0] < this.viewport[0][0]) && !(point[1] > this.viewport[0][1]) && !(point[0] > this.viewport[1][0]) && !(point[1] < this.viewport[1][1])) {
                if (flag) {
                    graphics.moveTo(translateXYtoPoint(point[0], point[1]).getX(), translateXYtoPoint(point[0], point[1]).getY());
                    flag = false;
                } else {
                    graphics.lineTo(translateXYtoPoint(point[0], point[1]).getX(), translateXYtoPoint(point[0], point[1]).getY());
                }
            }

        }
        canvas.draw(graphics);
    }


    protected void paintNullMarkers(Graphics2D canvas) {

        canvas.setStroke(markerStroke);


        canvas.setColor(Color.BLUE);
        Double[] firstpoint = graphicsData.getFirst();

        for (Double[] point : graphicsData) {


            if (Math.signum(firstpoint[1]) != Math.signum(point[1])) {
                // Преобразовать значения (x,y) в точку на экране point
                Double mod_y1 = abs(firstpoint[1]);
                Double mod_y2 = abs(point[1]);
                Double x1 = firstpoint[0];
                Double x2 = point[0];
                Double x0 = (mod_y1 * x2 + mod_y2 * x1) / (mod_y2 + mod_y1);
                Point2D.Double center = translateXYtoPoint(x0,
                        0);


                GeneralPath marker = new GeneralPath();


                marker.moveTo(center.getX() + 5.5, center.getY());
                marker.lineTo(center.getX() - 5.5, center.getY());
                marker.moveTo(center.getX(), center.getY() + 5.5);
                marker.lineTo(center.getX(), center.getY() - 5.5);
                marker.moveTo(center.getX() + 5.5, center.getY() + 4);
                marker.lineTo(center.getX() + 5.5, center.getY() - 4);
                marker.moveTo(center.getX() - 5.5, center.getY() + 4);
                marker.lineTo(center.getX() - 5.5, center.getY() - 4);
                marker.moveTo(center.getX() + 4, center.getY() + 5.5);
                marker.lineTo(center.getX() - 4, center.getY() + 5.5);
                marker.moveTo(center.getX() + 4, center.getY() - 5.5);
                marker.lineTo(center.getX() - 4, center.getY() - 5.5);
//

                canvas.draw(marker); // Начертить контур маркера
//            canvas.fill(marker); // Залить внутреннюю область маркера
                FontRenderContext context = canvas.getFontRenderContext();
                String x0_string = formatter.format(x0);

                Rectangle2D bounds = axisFont.getStringBounds(x0_string, context);
                Point2D.Double labelPos = translateXYtoPoint(x0, 0);
                // Вывести надпись в точке с вычисленными координатами

                canvas.drawString(x0_string, (float) (labelPos.getX() + 1), (float) (labelPos.getY() + bounds.getY()+30));
            }
            firstpoint = point;
        }

    }
    protected void paintMarkers(Graphics2D canvas) {
        // Шаг 1 - Установить специальное перо для черчения контуров маркеров
        canvas.setStroke(markerStroke);


        for (Double[] point : graphicsData) {

            double pd = (double)point[1];
            int p = (int)pd;
            if(p % 2 == 0){
                canvas.setColor(Color.BLACK);
            }
            else{
                canvas.setColor(Color.RED);
            }
            // Инициализировать эллипс как объект для представления маркера
            GeneralPath marker = new GeneralPath();
            Point2D.Double center = translateXYtoPoint(point[0],
                    point[1]);

            marker.moveTo(center.getX() + 5.5, center.getY());
            marker.lineTo(center.getX() - 5.5, center.getY());
            marker.moveTo(center.getX(), center.getY() + 5.5);
            marker.lineTo(center.getX(), center.getY() - 5.5);
            marker.moveTo(center.getX() + 5.5, center.getY()+4);
            marker.lineTo(center.getX() + 5.5, center.getY()-4);
            marker.moveTo(center.getX() - 5.5, center.getY()+4);
            marker.lineTo(center.getX() - 5.5, center.getY()-4);
            marker.moveTo(center.getX() + 4, center.getY() + 5.5);
            marker.lineTo(center.getX() - 4, center.getY() + 5.5);
            marker.moveTo(center.getX() + 4, center.getY() - 5.5);
            marker.lineTo(center.getX() - 4, center.getY() - 5.5);
//

            canvas.draw(marker); // Начертить контур маркера
//            canvas.fill(marker); // Залить внутреннюю область маркера
        }
    }

    private void paintLabels(Graphics2D canvas) {
        canvas.setColor(Color.BLACK);
        canvas.setFont(this.labelsFont);
        FontRenderContext context = canvas.getFontRenderContext();
        double labelYPos;
        if (this.viewport[1][1] < 0.0 && this.viewport[0][1] > 0.0) {
            labelYPos = 0.0;
        } else {
            labelYPos = this.viewport[1][1];
        }

        double labelXPos;
        if (this.viewport[0][0] < 0.0 && this.viewport[1][0] > 0.0) {
            labelXPos = 0.0;
        } else {
            labelXPos = this.viewport[0][0];
        }

        double pos = this.viewport[0][0];

        double step;
        Point2D.Double point;
        String label;
        Rectangle2D bounds;
        for(step = (this.viewport[1][0] - this.viewport[0][0]) / 10.0; pos < this.viewport[1][0]; pos += step) {
            point = this.translateXYtoPoint(pos, labelYPos);
            label = formatter.format(pos);
            bounds = this.labelsFont.getStringBounds(label, context);
            canvas.drawString(label, (float)(point.getX() + 5.0), (float)(point.getY() - bounds.getHeight()));
        }

        pos = this.viewport[1][1];

        for(step = (this.viewport[0][1] - this.viewport[1][1]) / 10.0; pos < this.viewport[0][1]; pos += step) {
            point = this.translateXYtoPoint(labelXPos, pos);
            label = formatter.format(pos);
            bounds = this.labelsFont.getStringBounds(label, context);
            canvas.drawString(label, (float)(point.getX() + 5.0), (float)(point.getY() - bounds.getHeight()));
        }

        if (this.selectedMarker >= 0) {
            point = this.translateXYtoPoint(((Double[])this.graphicsData.get(this.selectedMarker))[0], ((Double[])this.graphicsData.get(this.selectedMarker))[1]);
            label = "X=" + formatter.format(((Double[])this.graphicsData.get(this.selectedMarker))[0]) + ", Y=" + formatter.format(((Double[])this.graphicsData.get(this.selectedMarker))[1]);
            bounds = this.labelsFont.getStringBounds(label, context);
            canvas.setColor(Color.BLUE);
            canvas.drawString(label, (float)(point.getX() + 5.0), (float)(point.getY() - bounds.getHeight()));
        }

    }

    private void paintGrid(Graphics2D canvas) {
        canvas.setStroke(this.gridStroke);
        canvas.setColor(Color.GRAY);
        double pos = this.viewport[0][0];

        double step;
        for(step = (this.viewport[1][0] - this.viewport[0][0]) / 10.0; pos < this.viewport[1][0]; pos += step) {
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(pos, this.viewport[0][1]), this.translateXYtoPoint(pos, this.viewport[1][1])));
        }

        canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[1][0], this.viewport[0][1]), this.translateXYtoPoint(this.viewport[1][0], this.viewport[1][1])));
        pos = this.viewport[1][1];

        for(step = (this.viewport[0][1] - this.viewport[1][1]) / 10.0; pos < this.viewport[0][1]; pos += step) {
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], pos), this.translateXYtoPoint(this.viewport[1][0], pos)));
        }

        canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], this.viewport[0][1]), this.translateXYtoPoint(this.viewport[1][0], this.viewport[0][1])));
    }

    private void paintAxis(Graphics2D canvas) {
        canvas.setStroke(this.axisStroke);
        canvas.setColor(Color.BLACK);
        canvas.setFont(this.axisFont);
        FontRenderContext context = canvas.getFontRenderContext();
        Rectangle2D bounds;
        Point2D.Double labelPos;
        if (this.viewport[0][0] <= 0.0 && this.viewport[1][0] >= 0.0) {
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(0.0, this.viewport[0][1]), this.translateXYtoPoint(0.0, this.viewport[1][1])));
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(-(this.viewport[1][0] - this.viewport[0][0]) * 0.0025, this.viewport[0][1] - (this.viewport[0][1] - this.viewport[1][1]) * 0.015), this.translateXYtoPoint(0.0, this.viewport[0][1])));
            canvas.draw(new Line2D.Double(this.translateXYtoPoint((this.viewport[1][0] - this.viewport[0][0]) * 0.0025, this.viewport[0][1] - (this.viewport[0][1] - this.viewport[1][1]) * 0.015), this.translateXYtoPoint(0.0, this.viewport[0][1])));
            bounds = this.axisFont.getStringBounds("y", context);
            labelPos = this.translateXYtoPoint(0.0, this.viewport[0][1]);
            canvas.drawString("y", (float)labelPos.x + 10.0F, (float)(labelPos.y + bounds.getHeight() / 2.0));
        }

        if (this.viewport[1][1] <= 0.0 && this.viewport[0][1] >= 0.0) {
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], 0.0), this.translateXYtoPoint(this.viewport[1][0], 0.0)));
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[1][0] - (this.viewport[1][0] - this.viewport[0][0]) * 0.01, (this.viewport[0][1] - this.viewport[1][1]) * 0.005), this.translateXYtoPoint(this.viewport[1][0], 0.0)));
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[1][0] - (this.viewport[1][0] - this.viewport[0][0]) * 0.01, -(this.viewport[0][1] - this.viewport[1][1]) * 0.005), this.translateXYtoPoint(this.viewport[1][0], 0.0)));
            bounds = this.axisFont.getStringBounds("x", context);
            labelPos = this.translateXYtoPoint(this.viewport[1][0], 0.0);
            canvas.drawString("x", (float)(labelPos.x - bounds.getWidth() - 10.0), (float)(labelPos.y - bounds.getHeight() / 2.0));
        }

    }

    protected Point2D.Double translateXYtoPoint(double x, double y) {
        double deltaX = x - this.viewport[0][0];
        double deltaY = this.viewport[0][1] - y;
        return new Point2D.Double(deltaX * this.scaleX, deltaY * this.scaleY);
    }

    protected double[] translatePointToXY(int x, int y) {
        return new double[]{this.viewport[0][0] + (double)x / this.scaleX, this.viewport[0][1] - (double)y / this.scaleY};
    }

    protected int findSelectedPoint(int x, int y) {
        if (this.graphicsData == null) {
            return -1;
        } else {
            int pos = 0;

            for(Iterator var5 = this.graphicsData.iterator(); var5.hasNext(); ++pos) {
                Double[] point = (Double[])var5.next();
                Point2D.Double screenPoint = this.translateXYtoPoint(point[0], point[1]);
                double distance = (screenPoint.getX() - (double)x) * (screenPoint.getX() - (double)x) + (screenPoint.getY() - (double)y) * (screenPoint.getY() - (double)y);
                if (distance < 100.0) {
                    return pos;
                }
            }

            return -1;
        }
    }

    public void reset() {
        this.displayGraphics(this.originalData);
    }

    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }

    public void setShowNullMarkers(boolean showNullMarkers) {
        this.showNullMarkers = showNullMarkers;
        repaint();
    }

    public class MouseHandler extends MouseAdapter {
        public MouseHandler() {
        }

        public void mouseClicked(MouseEvent ev) {
            if (ev.getButton() == 3) {
//                if (GraphicsDisplay.this.undoHistory.size() > 0) {
//                    GraphicsDisplay.this.viewport = (double[][])GraphicsDisplay.this.undoHistory.get(GraphicsDisplay.this.undoHistory.size() - 1);
//                    GraphicsDisplay.this.undoHistory.remove(GraphicsDisplay.this.undoHistory.size() - 1);
//                } else {
                GraphicsDisplay.this.zoomToRegion(GraphicsDisplay.this.minX, GraphicsDisplay.this.maxY, GraphicsDisplay.this.maxX, GraphicsDisplay.this.minY);
//                }

                GraphicsDisplay.this.repaint();
            }

        }

        public void mousePressed(MouseEvent ev) {
            if (ev.getButton() == 1) {
                GraphicsDisplay.this.selectedMarker = GraphicsDisplay.this.findSelectedPoint(ev.getX(), ev.getY());
                GraphicsDisplay.this.originalPoint = GraphicsDisplay.this.translatePointToXY(ev.getX(), ev.getY());

                GraphicsDisplay.this.scaleMode = true;
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(5));
                GraphicsDisplay.this.selectionRect.setFrame((double)ev.getX(), (double)ev.getY(), 1.0, 1.0);
//                }

            }
        }

        public void mouseReleased(MouseEvent ev) {
            if (ev.getButton() == 1) {
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(0));
//                if (GraphicsDisplay.this.changeMode) {
//                    GraphicsDisplay.this.changeMode = false;
//                } else {
                GraphicsDisplay.this.scaleMode = false;
                double[] finalPoint = GraphicsDisplay.this.translatePointToXY(ev.getX(), ev.getY());
//                    GraphicsDisplay.this.undoHistory.add(GraphicsDisplay.this.viewport);
                GraphicsDisplay.this.viewport = new double[2][2];
                GraphicsDisplay.this.zoomToRegion(GraphicsDisplay.this.originalPoint[0], GraphicsDisplay.this.originalPoint[1], finalPoint[0], finalPoint[1]);
                GraphicsDisplay.this.repaint();
//                }

            }
        }
    }

    public class MouseMotionHandler implements MouseMotionListener {
        public MouseMotionHandler() {
        }

        public void mouseMoved(MouseEvent ev) {
            GraphicsDisplay.this.selectedMarker = GraphicsDisplay.this.findSelectedPoint(ev.getX(), ev.getY());


            GraphicsDisplay.this.repaint();
        }

        public void mouseDragged(MouseEvent ev) {

            double width = (double)ev.getX() - GraphicsDisplay.this.selectionRect.getX();
            if (width < 5.0) {
                width = 5.0;
            }

            double height = (double)ev.getY() - GraphicsDisplay.this.selectionRect.getY();
            if (height < 5.0) {
                height = 5.0;
            }

            GraphicsDisplay.this.selectionRect.setFrame(GraphicsDisplay.this.selectionRect.getX(), GraphicsDisplay.this.selectionRect.getY(), width, height);
            GraphicsDisplay.this.repaint();


        }
    }
}
