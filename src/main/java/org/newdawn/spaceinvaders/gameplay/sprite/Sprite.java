package org.newdawn.spaceinvaders.gameplay.sprite;

import java.awt.Graphics;
import java.awt.Image;

/**
 * 화면에 표시될 스프라이트입니다. 스프라이트는 상태 정보를 포함하지 않습니다.
 * 즉, 이미지일 뿐이고 위치는 아닙니다. 이를 통해 이미지의 여러 복사본을
 * 저장할 필요 없이 하나의 스프라이트를 많은 다른 곳에서 사용할 수 있습니다.
 * 
 * @author Kevin Glass
 */
public class Sprite {
	/** 이 스프라이트에 그려질 이미지 */
	private Image image;
	
	/**
	 * 이미지를 기반으로 새로운 스프라이트를 생성합니다
	 * 
	 * @param image 이 스프라이트인 이미지
	 */
	public Sprite(Image image) {
		this.image = image;
	}
	
	/**
	 * 그려진 스프라이트의 너비를 가져옵니다
	 * 
	 * @return 이 스프라이트의 픽셀 단위 너비
	 */
	public int getWidth() {
		return image.getWidth(null);
	}

	/**
	 * 그려진 스프라이트의 높이를 가져옵니다
	 * 
	 * @return 이 스프라이트의 픽셀 단위 높이
	 */
	public int getHeight() {
		return image.getHeight(null);
	}
	
	/**
	 * 이 스프라이트의 이미지를 가져옵니다
	 * 
	 * @return 이 스프라이트의 이미지
	 */
	public Image getImage() {
		return image;
	}
	
	/**
	 * Draw the sprite onto the graphics context provided
	 * 
	 * @param g The graphics context on which to draw the sprite
	 * @param x The x location at which to draw the sprite
	 * @param y The y location at which to draw the sprite
	 */
	public void draw(Graphics g,int x,int y) {
		g.drawImage(image,x,y,null);
	}
	
	/**
	 * Draw the sprite onto the graphics context provided with custom size
	 * 
	 * @param g The graphics context on which to draw the sprite
	 * @param x The x location at which to draw the sprite
	 * @param y The y location at which to draw the sprite
	 * @param width The width to draw the sprite
	 * @param height The height to draw the sprite
	 */
	public void draw(Graphics g,int x,int y,int width,int height) {
		g.drawImage(image,x,y,width,height,null);
	}
}