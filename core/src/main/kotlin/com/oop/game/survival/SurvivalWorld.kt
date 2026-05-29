package com.oop.game.survival

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.oop.game.GameWorld
import com.oop.game.InputHandler
import kotlin.math.floor

// 서바이벌 게임의 월드(맵, 스테이지) 클래스를 설정
class SurvivalWorld(
    screenWidth: Float,
    screenHeight: Float,
    worldWidth: Float,
    worldHeight: Float
) : GameWorld(screenWidth, screenHeight, worldWidth, worldHeight) {

    // 게임의 현재 상태를 나타내는 열거형을 설정 (플레이 중, 레벨업 선택, 게임오버)
    private enum class GameState { IN_PLAY, LEVEL_UP, GAME_OVER }

    // 플레이어 객체를 생성하고 월드 중앙에 배치하도록 설정
    private val player = SurvivalPlayer(
        x = worldWidth / 2 - 15f,
        y = worldHeight / 2 - 15f,
        worldWidth = worldWidth,
        worldHeight = worldHeight
    )

    // 게임의 초기 상태를 IN_PLAY로 설정
    private var state = GameState.IN_PLAY
    // 적 생성을 위한 타이머를 설정
    private var spawnTimer = 0f
    // 적 생성 간격을 1초로 설정
    private val spawnInterval = 1.0f

    // --- 배경 그리기용 리소스 ---
    private val tileTexture = Texture(Gdx.files.internal("tile.png"))
    private val bgColorDark = Color(0.08f, 0.08f, 0.08f, 1f)
    private val bgColorLight = Color(0.15f, 0.15f, 0.15f, 1f)
    private val tileSize = 64f
    // 반투명 배경을 그리기 위한 ShapeRenderer를 설정
    private val shapeRenderer = ShapeRenderer()

    // 월드 생성 시 플레이어를 게임 객체 목록에 추가하도록 설정
    init {
        add(player)
    }

    // 매 프레임 게임의 상태를 업데이트
    override fun update(delta: Float) {
        // 현재 게임 상태에 따라 다른 업데이트 함수를 호출
        when (state) {
            GameState.IN_PLAY -> updateInPlay(delta)
            GameState.LEVEL_UP -> updateLevelUp()
            GameState.GAME_OVER -> updateGameOver()
        }
    }

    // 게임이 플레이 중일 때의 로직을 처리
    private fun updateInPlay(delta: Float) {
        // 플레이어가 방금 레벨업 했는지 확인
        if (player.justLeveledUp) {
            // 레벨업 플래그를 초기화하고 게임 상태를 LEVEL_UP으로 변경
            player.justLeveledUp = false
            state = GameState.LEVEL_UP
            // 즉시 업데이트를 중단하여 게임을 정지
            return
        }

        // 스킬 발동 조건을 확인하고 처리
        handleSkillActivation()
        
        // 카메라가 플레이어를 따라다니도록 오프셋을 조정
        offsetX = player.x - screenWidth / 2
        offsetY = player.y - screenHeight / 2
        // 카메라가 월드 밖을 비추지 않도록 위치를 제한
        offsetX = offsetX.coerceIn(0f, worldWidth - screenWidth)
        offsetY = offsetY.coerceIn(0f, worldHeight - screenHeight)

        // 적 생성 타이머를 갱신
        spawnTimer += delta
        // 타이머가 1초를 넘으면 적을 생성하고 타이머를 리셋
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f
            spawnEnemy()
        }

        // 플레이어의 기본 공격 쿨타임이 다 찼는지 확인
        if (player.canAttack()) {
            // 가장 가까운 적을 찾아, 있으면 투사체를 발사
            findNearestEnemy()?.let { target ->
                val direction = Vector2(target.x - player.x, target.y - player.y)
                add(Projectile(player.x + player.width / 2, player.y + player.height / 2, direction, worldWidth, worldHeight))
                player.resetAttackCooldown()
            }
        }

        // 월드에 있는 모든 게임 객체들의 상태를 업데이트
        updateAllObjects(delta)
        // 충돌 관련 로직을 처리
        handleCollisions()
        // 죽은 객체들을 월드에서 제거
        removeDead()
    }

    // 스킬 발동을 처리하는 함수
    private fun handleSkillActivation() {
        // Ruin Flare 스킬 발동 조건을 확인
        if (player.canUseRuinFlare()) {
            // 가장 가까운 적을 찾아, 그 위치에 폭발을 생성
            findNearestEnemy()?.let { target ->
                add(Explosion(target.x + target.width / 2, target.y + target.height / 2, 80f, 15, 0.5f))
                player.resetRuinFlareCooldown()
            }
        }

        // Void Shift 스킬 발동 조건을 확인
        if (player.canUseVoidShift()) {
            // 순간이동 거리를 설정
            val shiftDistance = 120f
            // 현재 위치를 저장
            val originalX = player.x
            val originalY = player.y
            
            // 마지막 이동 방향으로 플레이어를 순간이동
            player.x += player.lastMovementDirection.x * shiftDistance
            player.y += player.lastMovementDirection.y * shiftDistance
            
            // 원래 위치에 잔상을 생성
            add(VoidShadow(originalX, originalY))
            player.resetVoidShiftCooldown()
        }
    }

    // 모든 충돌 판정을 처리하는 함수
    private fun handleCollisions() {
        // 현재 월드의 모든 객체 목록을 가져옴
        val objects = getObjects()
        // 객체들을 종류별로 분류
        val enemies = objects.filterIsInstance<TrackingEnemy>()
        val projectiles = objects.filterIsInstance<Projectile>()
        val explosions = objects.filterIsInstance<Explosion>()
        val voidShadows = objects.filterIsInstance<VoidShadow>()

        // 투사체와 적의 충돌을 처리
        projectiles.forEach { p ->
            if (!p.isAlive()) return@forEach
            enemies.find { e -> e.isAlive() && p.collidesWith(e) }?.let { enemy ->
                enemy.takeDamage(player.attackDamage)
                p.isDead = true
                if (!enemy.isAlive()) player.addExp(1)
            }
        }

        // 폭발과 적의 충돌을 처리
        explosions.forEach { explosion ->
            enemies.filter { it.isAlive() && !explosion.hitEnemies.contains(it) && explosion.collidesWith(it) }
                .forEach { enemy ->
                    enemy.takeDamage(explosion.damage)
                    // Ruin Flare 스킬이라면 화상 효과를 적용
                    if (player.activeSkills.contains("Ruin Flare")) enemy.applyBurn(3f)
                    // 중복 데미지를 막기 위해 맞은 적으로 기록
                    explosion.hitEnemies.add(enemy)
                    if (!enemy.isAlive()) player.addExp(1)
                }
        }
        
        // Void Shift 잔상이 사라질 때 폭발을 생성
        voidShadows.filter { !it.isAlive() }.forEach { shadow ->
            add(Explosion(shadow.x + shadow.width / 2, shadow.y + shadow.height / 2, 60f, 10, 0.3f))
        }

        // 적과 플레이어의 충돌을 처리
        enemies.find { it.isAlive() && player.collidesWith(it) }?.let {
            player.takeDamage(10)
            if (player.hp <= 0) state = GameState.GAME_OVER
        }
    }

    // 레벨업 상태일 때 스킬 선택 로직을 처리
    private fun updateLevelUp() {
        if (InputHandler.isKeyJustPressed(InputHandler.NUM_1)) { player.activeSkills.add("Shadow Veil"); state = GameState.IN_PLAY }
        if (InputHandler.isKeyJustPressed(InputHandler.NUM_2)) { player.activeSkills.add("Ruin Flare"); state = GameState.IN_PLAY }
        if (InputHandler.isKeyJustPressed(InputHandler.NUM_3)) { player.activeSkills.add("Void Shift"); state = GameState.IN_PLAY }
    }

    // 가장 가까운 적을 찾아 반환하는 함수
    private fun findNearestEnemy(): TrackingEnemy? = getObjects().filterIsInstance<TrackingEnemy>().filter { it.isAlive() }.minByOrNull { Vector2.dst2(player.x, player.y, it.x, it.y) }

    // 적을 생성하는 함수
    private fun spawnEnemy() {
        val spawnPos = Vector2()
        // 화면 바깥 4방향 중 한 곳을 무작위로 선택
        val side = MathUtils.random(3)
        when (side) {
            0 -> { spawnPos.set(MathUtils.random(offsetX, offsetX + screenWidth), offsetY + screenHeight + 50f) } // 위
            1 -> { spawnPos.set(MathUtils.random(offsetX, offsetX + screenWidth), offsetY - 50f) } // 아래
            2 -> { spawnPos.set(offsetX - 50f, MathUtils.random(offsetY, offsetY + screenHeight)) } // 왼쪽
            else -> { spawnPos.set(offsetX + screenWidth + 50f, MathUtils.random(offsetY, offsetY + screenHeight)) } // 오른쪽
        }
        // 선택된 위치에 적을 생성하고 월드에 추가
        add(TrackingEnemy(spawnPos.x, spawnPos.y, player))
    }

    // 게임오버 상태일 때 로직을 처리
    private fun updateGameOver() { if (InputHandler.isKeyJustPressed(InputHandler.ESCAPE)) Gdx.app.exit() }

    // 배경을 그리는 함수
    override fun drawBackground(batch: SpriteBatch) {
        // 화면에 보이는 영역만큼만 타일을 그리도록 계산
        val startCol = floor(offsetX / tileSize).toInt() - 1
        val startRow = floor(offsetY / tileSize).toInt() - 1
        val cols = (screenWidth / tileSize).toInt() + 3
        val rows = (screenHeight / tileSize).toInt() + 3
        // 체스판 무늬로 타일을 그림
        for (row in startRow until startRow + rows) {
            for (col in startCol until startCol + cols) {
                batch.color = if ((row + col) % 2 == 0) bgColorDark else bgColorLight
                val drawX = col * tileSize - offsetX
                val drawY = row * tileSize - offsetY
                batch.draw(tileTexture, drawX, drawY, tileSize, tileSize)
            }
        }
        // 다음 그리기에 영향이 없도록 색상을 흰색으로 복원
        batch.color = Color.WHITE
    }

    // 매 프레임 화면을 그리는 메인 함수
    override fun render(delta: Float) {
        // 1. 게임 세계 그리기 (배경, 객체들)
        super.render(delta)

        // 2. UI 그리기 (좌표계 분리)
        // UI를 그리기 위해 화면 좌표계로 전환
        batch.projectionMatrix.setToOrtho2D(0f, 0f, screenWidth, screenHeight)
        
        // HUD(체력, 레벨 등)를 그림
        batch.begin()
        font.color = Color.YELLOW
        font.data.setScale(1.2f)
        font.draw(batch, "LV: ${player.level}   EXP: ${player.exp} / ${player.getRequiredExp(player.level)}", 10f, screenHeight - 10f)
        font.color = Color.RED
        font.draw(batch, "HP: ${player.hp} / ${player.maxHp}", 10f, screenHeight - 40f)
        batch.end()

        // 게임 상태에 따라 추가 UI를 그림
        if (state == GameState.LEVEL_UP) {
            drawLevelUpScreen()
        } else if (state == GameState.GAME_OVER) {
            drawGameOverOverlay()
        }
    }
    
    // 레벨업 선택 화면을 그리는 함수
    private fun drawLevelUpScreen() {
        // 반투명 배경을 그리기 위해 블렌딩 옵션을 활성화
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        // 도형을 그리기 위해 화면 좌표계로 전환
        shapeRenderer.projectionMatrix.setToOrtho2D(0f, 0f, screenWidth, screenHeight)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0f, 0f, 0f, 0.7f)
        shapeRenderer.rect(0f, 0f, screenWidth, screenHeight)
        shapeRenderer.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)

        // 스킬 선택 텍스트를 그림
        batch.begin()
        font.color = Color.YELLOW
        font.data.setScale(2f)
        font.draw(batch, "LEVEL UP!", screenWidth / 2 - 100f, screenHeight * 0.8f)
        
        font.data.setScale(1.5f)
        font.color = Color.WHITE
        font.draw(batch, "Choose your skill:", screenWidth / 2 - 120f, screenHeight * 0.7f)
        
        font.data.setScale(1.2f)
        font.color = Color.CYAN
        font.draw(batch, "1. Shadow Veil", screenWidth / 2 - 150f, screenHeight * 0.5f)
        font.data.setScale(1f)
        font.color = Color.GRAY
        font.draw(batch, "   Increases evasion for a short time.", screenWidth / 2 - 150f, screenHeight * 0.5f - 30f)

        font.data.setScale(1.2f)
        font.color = Color.RED
        font.draw(batch, "2. Ruin Flare", screenWidth / 2 - 150f, screenHeight * 0.4f)
        font.data.setScale(1f)
        font.color = Color.GRAY
        font.draw(batch, "   Causes an explosion with damage over time.", screenWidth / 2 - 150f, screenHeight * 0.4f - 30f)

        font.data.setScale(1.2f)
        font.color = Color.PURPLE
        font.draw(batch, "3. Void Shift", screenWidth / 2 - 150f, screenHeight * 0.3f)
        font.data.setScale(1f)
        font.color = Color.GRAY
        font.draw(batch, "   Teleports and leaves an explosive afterimage.", screenWidth / 2 - 150f, screenHeight * 0.3f - 30f)
        batch.end()
    }

    // 게임오버 화면을 그리는 함수
    private fun drawGameOverOverlay() {
        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(2f)
        font.draw(batch, "Game Over!", screenWidth / 2 - 80f, screenHeight / 2)
        font.data.setScale(1f)
        font.draw(batch, "Press ESC to exit", screenWidth / 2 - 70f, screenHeight / 2 - 40f)
        batch.end()
    }

    // 객체 소멸 시 모든 자원을 해제
    override fun dispose() {
        super.dispose()
        tileTexture.dispose()
        shapeRenderer.dispose()
    }
}