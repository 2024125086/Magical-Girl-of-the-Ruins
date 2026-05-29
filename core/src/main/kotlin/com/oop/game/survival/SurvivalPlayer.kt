package com.oop.game.survival

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.oop.game.GameObject
import com.oop.game.InputHandler
import kotlin.random.Random

// 서바이벌 게임의 플레이어 클래스를 설정
class SurvivalPlayer(
    x: Float,
    y: Float,
    private val worldWidth: Float,
    private val worldHeight: Float
) : GameObject(x, y, 30f, 30f) {

    // 플레이어 이미지를 설정
    private val texture = Texture(Gdx.files.internal("player.png"))
    // 플레이어의 이동 속도를 설정
    private val speed = 200f

    // --- 플레이어 스탯 ---
    // 최대 체력을 설정
    var maxHp: Int = 100
    // 현재 체력을 설정
    var hp: Int = 100
    // 현재 레벨을 설정
    var level: Int = 1
    // 현재 경험치를 설정
    var exp: Int = 0

    // 피격 후 무적 시간을 계산하는 타이머를 설정
    private var invincibilityTimer = 0f
    // 무적 시간 지속시간을 0.5초로 설정
    private val invincibilityDuration = 0.5f

    // 기본 공격의 데미지를 설정
    var attackDamage = 4
    // 공격 쿨타임을 계산하는 타이머를 설정
    private var attackCooldown = 0f
    // 공격 간격을 0.4초로 설정
    private val attackInterval = 0.4f

    // 획득한 스킬 목록을 저장하는 Set을 설정 (중복 획득 방지)
    val activeSkills = mutableSetOf<String>()
    // 레벨업 직후인지 알려주는 플래그를 설정
    var justLeveledUp = false

    // --- 스킬 관련 변수 ---
    // Shadow Veil 스킬이 활성화 상태인지 설정
    var isShadowVeilActive = false
    // Shadow Veil 스킬의 전체 쿨타임을 8초로 설정
    private var shadowVeilCooldown = 8f
    // Shadow Veil 스킬의 지속시간을 4초로 설정
    private var shadowVeilDuration = 4f
    
    // Ruin Flare 스킬의 쿨타임을 4초로 설정
    private var ruinFlareCooldown = 4f

    // Void Shift 스킬의 쿨타임을 6초로 설정
    private var voidShiftCooldown = 6f
    // 마지막으로 이동한 방향을 저장 (Void Shift 순간이동 방향으로 사용)
    val lastMovementDirection = Vector2(1f, 0f)

    // 매 프레임 플레이어의 상태를 갱신
    override fun update(delta: Float) {
        // 키보드 입력에 따른 이동 방향을 계산
        val moveX = (if (InputHandler.isKeyPressed(InputHandler.RIGHT)) 1f else 0f) - (if (InputHandler.isKeyPressed(InputHandler.LEFT)) 1f else 0f)
        val moveY = (if (InputHandler.isKeyPressed(InputHandler.UP)) 1f else 0f) - (if (InputHandler.isKeyPressed(InputHandler.DOWN)) 1f else 0f)
        
        // 이동 방향 벡터를 정규화 (대각선 이동 속도 보정)
        val moveDirection = Vector2(moveX, moveY).nor()
        // 이동 방향이 있을 경우에만 위치를 업데이트
        if (!moveDirection.isZero) {
            x += moveDirection.x * speed * delta
            y += moveDirection.y * speed * delta
            // 마지막 이동 방향을 갱신
            lastMovementDirection.set(moveDirection)
        }

        // 플레이어가 월드 밖으로 나가지 않도록 위치를 제한
        x = x.coerceIn(0f, worldWidth - width)
        y = y.coerceIn(0f, worldHeight - height)

        // 각종 타이머들을 갱신
        if (invincibilityTimer > 0f) invincibilityTimer -= delta
        if (attackCooldown > 0f) attackCooldown -= delta
        updateSkillCooldowns(delta)
    }
    
    // 스킬들의 쿨타임을 관리
    private fun updateSkillCooldowns(delta: Float) {
        // Shadow Veil 스킬을 배웠는지 확인
        if (activeSkills.contains("Shadow Veil")) {
            // 쿨타임을 감소시킴
            shadowVeilCooldown -= delta
            // 쿨타임이 다 되면 스킬을 활성화
            if (shadowVeilCooldown <= 0f) {
                isShadowVeilActive = true
                // 전체 쿨타임을 다시 8초로 설정 (지속 4초 + 비활성 4초)
                shadowVeilCooldown = 8f 
            }
            // 스킬이 활성화된 상태라면 지속시간을 감소시킴
            if (isShadowVeilActive) {
                shadowVeilDuration -= delta
                // 지속시간이 다 되면 스킬을 비활성화
                if (shadowVeilDuration <= 0f) {
                    isShadowVeilActive = false
                    // 지속시간을 다시 4초로 재충전
                    shadowVeilDuration = 4f 
                }
            }
        }
        // Ruin Flare 스킬을 배웠는지 확인하고 쿨타임을 감소
        if (activeSkills.contains("Ruin Flare")) {
            ruinFlareCooldown -= delta
        }
        // Void Shift 스킬을 배웠는지 확인하고 쿨타임을 감소
        if (activeSkills.contains("Void Shift")) {
            voidShiftCooldown -= delta
        }
    }

    // 플레이어를 화면에 그림
    override fun draw(batch: SpriteBatch) {
        // 무적 상태일 때 0.1초 간격으로 깜빡이도록 설정
        if (invincibilityTimer > 0f && (invincibilityTimer * 10).toInt() % 2 == 0) return
        
        // 그리기 전 기존 색상을 백업
        val oldColor = batch.color
        // Shadow Veil이 활성화 상태이면 플레이어 색상을 푸른빛으로 변경
        if (isShadowVeilActive) {
            batch.color = Color(0.7f, 0.7f, 1f, 0.8f)
        }
        // 화면에 플레이어를 그림
        batch.draw(texture, x, y, width, height)
        // 기존 색상으로 복원
        batch.color = oldColor
    }

    // 객체 소멸 시 텍스처 자원을 해제
    override fun dispose() {
        texture.dispose()
    }

    // 피해를 받는 함수
    fun takeDamage(amount: Int) {
        // 무적 상태이면 피해를 받지 않음
        if (invincibilityTimer > 0f) return
        
        // Shadow Veil 스킬의 회피 효과를 적용
        if (isShadowVeilActive && Random.nextFloat() < 0.5f) {
            return // 50% 확률로 데미지를 무시
        }

        // 체력을 감소시키고 무적 시간을 부여
        hp -= amount
        invincibilityTimer = invincibilityDuration
        // 체력이 0 미만으로 내려가지 않도록 설정
        if (hp < 0) hp = 0
    }

    // 경험치를 획득하는 함수
    fun addExp(amount: Int) {
        exp += amount
        // 레벨업 조건을 확인
        checkLevelUp()
    }

    // 레벨업 조건을 확인하는 함수
    private fun checkLevelUp() {
        // 현재 레벨에 필요한 경험치를 가져옴
        val reqExp = getRequiredExp(level)
        // 현재 경험치가 필요 경험치보다 많으면 레벨업
        if (exp >= reqExp) {
            // 경험치를 소모하고 레벨업 함수를 호출
            exp -= reqExp
            levelUp()
            // 월드에 레벨업 했음을 알리는 플래그를 설정
            justLeveledUp = true
        }
    }

    // 레벨별 필요 경험치를 반환하는 함수
    fun getRequiredExp(currentLevel: Int): Int {
        return when (currentLevel) {
            in 1..5 -> 10; in 6..10 -> 20; in 11..15 -> 30; else -> 40
        }
    }

    // 레벨업 시 스탯을 상승시키는 함수
    private fun levelUp() {
        // 레벨 구간에 따라 체력 증가량을 설정
        val hpIncrease = when (level) {
            in 1..5 -> 10; in 6..10 -> 5; in 11..15 -> 3; else -> 1
        }
        // 레벨을 1 올림
        level++
        // 최대 체력을 증가시킴
        maxHp += hpIncrease
        // 현재 체력도 회복시켜 줌
        hp += hpIncrease
    }

    // 기본 공격이 가능한지 확인하는 함수
    fun canAttack(): Boolean = attackCooldown <= 0f
    // 기본 공격 쿨타임을 초기화하는 함수
    fun resetAttackCooldown() { attackCooldown = attackInterval }
    
    // Ruin Flare 스킬 사용이 가능한지 확인하는 함수
    fun canUseRuinFlare(): Boolean = activeSkills.contains("Ruin Flare") && ruinFlareCooldown <= 0f
    // Ruin Flare 스킬 쿨타임을 초기화하는 함수
    fun resetRuinFlareCooldown() { ruinFlareCooldown = 4f }

    // Void Shift 스킬 사용이 가능한지 확인하는 함수
    fun canUseVoidShift(): Boolean = activeSkills.contains("Void Shift") && voidShiftCooldown <= 0f
    // Void Shift 스킬 쿨타임을 초기화하는 함수
    fun resetVoidShiftCooldown() { voidShiftCooldown = 6f }
}