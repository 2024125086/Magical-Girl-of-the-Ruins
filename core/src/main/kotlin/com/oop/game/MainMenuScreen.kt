package com.oop.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class MainMenuScreen(
    screenWidth: Float,
    screenHeight: Float,
    private val game: OopGame
) : GameWorld(screenWidth, screenHeight, screenWidth, screenHeight) {

    private val backgroundTexture = Texture(Gdx.files.internal("main_bg.png"))
    private val bannerTexture = Texture(Gdx.files.internal("banner_12.png"))

    override fun drawBackground(batch: SpriteBatch) {

        batch.draw(backgroundTexture, 0f, 0f, screenWidth, screenHeight)

        val originalBannerWidth = bannerTexture.width.toFloat()
        val originalBannerHeight = bannerTexture.height.toFloat()


        val scale = 0.05f
        val reducedWidth = originalBannerWidth * scale
        val reducedHeight = originalBannerHeight * scale


        val bannerX = screenWidth - reducedWidth
        val bannerY = screenHeight - reducedHeight

        batch.draw(bannerTexture, bannerX, bannerY, reducedWidth, reducedHeight)
    }

    override fun update(delta: Float) {
        super.update(delta)


        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER) ||
            Gdx.input.justTouched()) {


            println("시작 신호 감지!")
        }
    }

    override fun render(delta: Float) {

        super.render(delta)

        drawTextOnScreen(
            text = "MAGICAL GIRL OF THE RUINS",
            x = screenWidth / 2f - 250f,
            y = screenHeight * 0.8f,
            color = Color.BLACK,
            scale = 2f
        )

        drawTextOnScreen(
            text = "Press Any Key",
            x = screenWidth / 2f - 80f,
            y = screenHeight * 0.33f,
            color = Color.WHITE,
            scale = 1.2f
        )
    }

    override fun dispose() {
        super.dispose()
        backgroundTexture.dispose()
        bannerTexture.dispose()
    }
}