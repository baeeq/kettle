package org.pentaho.di.core.gui;

import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.NotePadMeta;
import org.pentaho.di.core.gui.Point;
import org.pentaho.di.core.gui.Rectangle;
import org.pentaho.di.core.gui.ScrollBarInterface;
import org.pentaho.di.core.gui.AreaOwner.AreaType;
import org.pentaho.di.core.gui.GCInterface.EColor;
import org.pentaho.di.core.gui.GCInterface.EImage;
import org.pentaho.di.core.gui.GCInterface.ELineStyle;
import org.pentaho.di.trans.step.errorhandling.StreamIcon;

public class BasePainter {
	
	public final double theta = Math.toRadians(11); // arrowhead sharpness

	protected static final int  MINI_ICON_SIZE = 16;
	protected static final int	MINI_ICON_MARGIN = 5;
	protected static final int	MINI_ICON_TRIANGLE_BASE = 10;
	protected static final int	MINI_ICON_DISTANCE = 7;
	protected static final int	MINI_ICON_SKEW = 0;
	
	
	
    protected Point        area;
    
    protected ScrollBarInterface    hori, vert;
    
    protected List<AreaOwner> areaOwners;

    protected int          shadowsize;
    protected Point        offset;
    protected Point        drop_candidate;
    protected int          iconsize;
    protected int          gridSize;
    protected Rectangle    selrect;
    protected int          linewidth;
    protected float        magnification;
    protected float        translationX;
    protected float        translationY;
	protected boolean 	   shadow;

	protected Object	   subject;

	protected GCInterface	gc;

	protected int	shadowSize;

	private String	noteFontName;

	private int	noteFontHeight;

    public BasePainter(GCInterface gc, Object subject, Point area, ScrollBarInterface hori, ScrollBarInterface vert, Point drop_candidate, Rectangle selrect, List<AreaOwner> areaOwners, 
    		int iconsize, int linewidth, int gridsize, int shadowSize, boolean antiAliasing, 
    		String noteFontName, int noteFontHeight
    		) {
    	this.gc = gc;
    	this.subject        = subject;
        this.area           = area;
        this.hori           = hori;
        this.vert           = vert;

        this.selrect        = selrect;
        this.drop_candidate = drop_candidate;
        
        this.areaOwners = areaOwners;
        areaOwners.clear(); // clear it before we start filling it up again.

        // props = PropsUI.getInstance();
        this.iconsize = iconsize; 
        this.linewidth = linewidth;
        this.gridSize = gridsize;

        this.shadowSize = shadowSize;
        this.shadow = shadowSize>0;
        
        this.magnification = 1.0f;
        
        gc.setAntialias(antiAliasing);
        
        this.noteFontName = noteFontName;
        this.noteFontHeight = noteFontHeight;
	}

	public static EImage getStreamIconImage(StreamIcon streamIcon) {
		switch(streamIcon) {
		case TRUE   : return EImage.TRUE;
		case FALSE  : return EImage.FALSE;
		case ERROR  : return EImage.ERROR;
		case INFO   : return EImage.INFO;
		case TARGET : return EImage.TARGET;
		case INPUT  : return EImage.INPUT;
		case OUTPUT : return EImage.OUTPUT;
		default:      return EImage.ARROW;
		}
    }
    
	protected void drawNote(NotePadMeta notePadMeta)
    {
		if (notePadMeta.isSelected()) {
			gc.setLineWidth(2);
		} else {
			gc.setLineWidth(1);
		}
        
        Point ext;
        if (Const.isEmpty(notePadMeta.getNote()))
        {
            ext = new Point(10,10); // Empty note
        }
        else
        {

            gc.setFont(Const.NVL(notePadMeta.getFontName(),noteFontName), 
            		notePadMeta.getFontSize()==-1 ? noteFontHeight : notePadMeta.getFontSize(),
            				notePadMeta.isFontBold(),
            				notePadMeta.isFontItalic()
            				);
            
            ext = gc.textExtent(notePadMeta.getNote());
        }
        Point p = new Point(ext.x, ext.y);
        Point loc = notePadMeta.getLocation();
        Point note = real2screen(loc.x, loc.y, offset);
        int margin = Const.NOTE_MARGIN;
        p.x += 2 * margin;
        p.y += 2 * margin;
        int width = notePadMeta.width;
        int height = notePadMeta.height;
        if (p.x > width) width = p.x;
        if (p.y > height) height = p.y;

        int noteshape[] = new int[] { note.x, note.y, // Top left
                note.x + width + 2 * margin, note.y, // Top right
                note.x + width + 2 * margin, note.y + height, // bottom right 1
                note.x + width, note.y + height + 2 * margin, // bottom right 2
                note.x + width, note.y + height, // bottom right 3
                note.x + width + 2 * margin, note.y + height, // bottom right 1
                note.x + width, note.y + height + 2 * margin, // bottom right 2
                note.x, note.y + height + 2 * margin // bottom left
        };
		
		// Draw shadow around note?
		if(notePadMeta.isDrawShadow())
		{
			int s = shadowsize;
			int shadowa[] = new int[] { note.x+s, note.y+s, // Top left
				note.x + width + 2 * margin+s, note.y+s, // Top right
				note.x + width + 2 * margin+s, note.y + height+s, // bottom right 1
				note.x + width+s, note.y + height + 2 * margin+s, // bottom right 2
				note.x+s, note.y + height + 2 * margin+s // bottom left
			};
			gc.setBackground(EColor.LIGHTGRAY);
			gc.fillPolygon(shadowa);
		}
        gc.setBackground(notePadMeta.getBackGroundColorRed(), notePadMeta.getBackGroundColorGreen(), notePadMeta.getBackGroundColorBlue());
        gc.setForeground(notePadMeta.getBorderColorRed(), notePadMeta.getBorderColorGreen(), notePadMeta.getBorderColorBlue());

        gc.fillPolygon(noteshape);
        gc.drawPolygon(noteshape);
      
        if (!Const.isEmpty(notePadMeta.getNote()))
        {
            gc.setForeground(notePadMeta.getFontColorRed(),notePadMeta.getFontColorGreen(),notePadMeta.getFontColorBlue());
        	gc.drawText(notePadMeta.getNote(), note.x + margin, note.y + margin, true);
        }
   
        notePadMeta.width = width; // Save for the "mouse" later on...
        notePadMeta.height = height;

        if (notePadMeta.isSelected()) gc.setLineWidth(1); else gc.setLineWidth(2);
        
        // Add to the list of areas...
        //
        if (!shadow) {
        	areaOwners.add(new AreaOwner(AreaType.NOTE, note.x, note.y, width, height, subject, notePadMeta));
        }
    }
	
    protected static final Point real2screen(int x, int y, Point offset)
    {
        Point screen = new Point(x + offset.x, y + offset.y);

        return screen;
    }

    protected Point getThumb(Point area, Point transMax)
    {
    	Point resizedMax = magnifyPoint(transMax);
    	
        Point thumb = new Point(0, 0);
        if (resizedMax.x <= area.x)
            thumb.x = 100;
        else
            thumb.x = 100 * area.x / resizedMax.x;

        if (resizedMax.y <= area.y)
            thumb.y = 100;
        else
            thumb.y = 100 * area.y / resizedMax.y;

        return thumb;
    }
    
    protected Point magnifyPoint(Point p) {
    	return new Point(Math.round(p.x * magnification), Math.round(p.y*magnification));
    }
    
    protected Point getOffset(Point thumb, Point area)
    {
        Point p = new Point(0, 0);

        if (hori==null || vert==null) return p;

        Point sel = new Point(hori.getSelection(), vert.getSelection());

        if (thumb.x == 0 || thumb.y == 0) return p;

        p.x = -sel.x * area.x / thumb.x;
        p.y = -sel.y * area.y / thumb.y;

        return p;
    }
    
    protected void drawRect(Rectangle rect)
    {
        if (rect == null) return;
        gc.setLineStyle(ELineStyle.DASHDOT);
        gc.setLineWidth(linewidth);
        gc.setForeground(EColor.GRAY);
        // PDI-2619: SWT on Windows doesn't cater for negative rect.width/height so handle here. 
        Point s = real2screen(rect.x, rect.y, offset);
        if (rect.width < 0) {
        	s.x = s.x + rect.width;
        }
        if (rect.height < 0) {
        	s.y = s.y + rect.height;
        }
        gc.drawRectangle(s.x, s.y, Math.abs(rect.width), Math.abs(rect.height));
        gc.setLineStyle(ELineStyle.SOLID);
    }

    protected void drawGrid() {
    	Point bounds = gc.getDeviceBounds();
		for (int x=0;x<bounds.x;x+=gridSize) {
			for (int y=0;y<bounds.y;y+=gridSize) {
				gc.drawPoint(x+(offset.x%gridSize),y+(offset.y%gridSize));
			}
		}
	}

    
    protected int calcArrowLength() {
    	return 19 + (linewidth - 1) * 5; // arrowhead length;
    }
    
    
	/**
	 * @return the magnification
	 */
	public float getMagnification() {
		return magnification;
	}

	/**
	 * @param magnification the magnification to set
	 */
	public void setMagnification(float magnification) {
		this.magnification = magnification;
	}
}
