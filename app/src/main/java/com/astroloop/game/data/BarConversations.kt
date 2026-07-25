package com.astroloop.game.data

import com.astroloop.game.hangar.ChatMessage

data class BarConversation(
    val participantIds: List<String>,
    val lines: List<ChatMessage>,
    val requiresArcCompleted: Boolean = false,
    val blockedInAstroLoop: Boolean = false,
    val requiresAstroLoop: Boolean = false
) {
    // Backward-compatible 2-pilot constructor — keeps the 254 existing entries
    // and getShieldDiscoveryConversation()'s named-arg call compiling unchanged.
    constructor(
        pilotAId: String,
        pilotBId: String,
        lines: List<ChatMessage>,
        requiresArcCompleted: Boolean = false,
        blockedInAstroLoop: Boolean = false,
        requiresAstroLoop: Boolean = false
    ) : this(
        listOf(pilotAId, pilotBId), lines,
        requiresArcCompleted, blockedInAstroLoop, requiresAstroLoop
    )
}

object BarConversations {

    private val conversations: List<BarConversation> = listOf(

        // ============================================================
        // === MEDIC + RASCAL ===
        // ============================================================
        BarConversation("pilot_medic", "pilot_rascal", listOf(
            ChatMessage("MEDIC", "Rascal, why are my bandages crammed in the vents?", 0xFFFF88AA.toInt()),
            ChatMessage("RASCAL", "They make great insulation! Real cozy up there now.", 0xFFDDAA33.toInt()),
            ChatMessage("MEDIC", "Those were sterile, single-use surgical supplies.", 0xFFFF88AA.toInt()),
            ChatMessage("RASCAL", "Were. They were sterile.", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_medic", "pilot_rascal", listOf(
            ChatMessage("RASCAL", "Hey Medic, what's this weird rash on my arm?", 0xFFDDAA33.toInt()),
            ChatMessage("MEDIC", "That's grease. You're covered in grease.", 0xFFFF88AA.toInt()),
            ChatMessage("RASCAL", "Oh good. I thought it was serious.", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_medic", "pilot_rascal", listOf(
            ChatMessage("MEDIC", "Stop eating things you find on the floor.", 0xFFFF88AA.toInt()),
            ChatMessage("RASCAL", "In my defense, it was on a shelf.", 0xFFDDAA33.toInt()),
            ChatMessage("MEDIC", "That was a labeled medical sample, Rascal.", 0xFFFF88AA.toInt()),
            ChatMessage("RASCAL", "...tasted fine.", 0xFFDDAA33.toInt())
        )),

        // ============================================================
        // === MEDIC + BRUTUS ===
        // ============================================================
        BarConversation("pilot_medic", "pilot_brutus", listOf(
            ChatMessage("MEDIC", "Brutus, you need to let me treat that wound.", 0xFFFF88AA.toInt()),
            ChatMessage("BRUTUS", "It's fine.", 0xFF77AA33.toInt()),
            ChatMessage("MEDIC", "You have actual shrapnel lodged in your shoulder.", 0xFFFF88AA.toInt()),
            ChatMessage("BRUTUS", "Adds character.", 0xFF77AA33.toInt())
        )),
        BarConversation("pilot_medic", "pilot_brutus", listOf(
            ChatMessage("MEDIC", "Brutus, when was your last actual checkup?", 0xFFFF88AA.toInt()),
            ChatMessage("BRUTUS", "Never.", 0xFF77AA33.toInt()),
            ChatMessage("MEDIC", "That would explain everything in the X-ray.", 0xFFFF88AA.toInt())
        )),
        BarConversation("pilot_medic", "pilot_brutus", listOf(
            ChatMessage("BRUTUS", "Doc.", 0xFF77AA33.toInt()),
            ChatMessage("MEDIC", "Brutus. Are you here for treatment?", 0xFFFF88AA.toInt()),
            ChatMessage("BRUTUS", "No. Drink.", 0xFF77AA33.toInt()),
            ChatMessage("MEDIC", "You know I'm not the bartender, right?", 0xFFFF88AA.toInt())
        )),

        // ============================================================
        // === MEDIC + FROST ===
        // ============================================================
        BarConversation("pilot_medic", "pilot_frost", listOf(
            ChatMessage("MEDIC", "Frost, your body temp is reading below 'alive.'", 0xFFFF88AA.toInt()),
            ChatMessage("FROST", "For a mammal, perhaps. I am, however, a penguin.", 0xFF55BBFF.toInt()),
            ChatMessage("MEDIC", "Fair point. Carry on.", 0xFFFF88AA.toInt())
        )),
        BarConversation("pilot_medic", "pilot_frost", listOf(
            ChatMessage("FROST", "My analysis suggests you need sleep, doctor.", 0xFF55BBFF.toInt()),
            ChatMessage("MEDIC", "Bold of you to diagnose the doctor.", 0xFFFF88AA.toInt()),
            ChatMessage("FROST", "Someone has to. You won't.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_medic", "pilot_frost", listOf(
            ChatMessage("MEDIC", "Your feathers keep clogging the med-bay drain.", 0xFFFF88AA.toInt()),
            ChatMessage("FROST", "I molt under stress. It's involuntary.", 0xFF55BBFF.toInt()),
            ChatMessage("MEDIC", "You were just calmly reading a book.", 0xFFFF88AA.toInt()),
            ChatMessage("FROST", "A stressful book.", 0xFF55BBFF.toInt())
        )),

        // ============================================================
        // === MEDIC + DASH ===
        // ============================================================
        BarConversation("pilot_medic", "pilot_dash", listOf(
            ChatMessage("DASH", "Medic! I think I pulled something important!", 0xFFFFDD22.toInt()),
            ChatMessage("MEDIC", "Again? What were you doing this time?", 0xFFFF88AA.toInt()),
            ChatMessage("DASH", "Seeing if I could lap the hangar in nine seconds.", 0xFFFFDD22.toInt()),
            ChatMessage("MEDIC", "Could you?", 0xFFFF88AA.toInt())
        )),
        BarConversation("pilot_medic", "pilot_dash", listOf(
            ChatMessage("MEDIC", "Dash, hold still for one second. I need your pulse.", 0xFFFF88AA.toInt()),
            ChatMessage("DASH", "It's fast. Trust me.", 0xFFFFDD22.toInt()),
            ChatMessage("MEDIC", "I need an actual number, not a vibe.", 0xFFFF88AA.toInt()),
            ChatMessage("DASH", "Higher than yours. Can I go now?", 0xFFFFDD22.toInt())
        )),
        BarConversation("pilot_medic", "pilot_dash", listOf(
            ChatMessage("DASH", "Does caffeine technically count as medicine?", 0xFFFFDD22.toInt()),
            ChatMessage("MEDIC", "Not the way you use it.", 0xFFFF88AA.toInt()),
            ChatMessage("DASH", "What if I just double the dose then?", 0xFFFFDD22.toInt())
        )),

        // ============================================================
        // === MEDIC + EMBER ===
        // ============================================================
        BarConversation("pilot_medic", "pilot_ember", listOf(
            ChatMessage("MEDIC", "Third-degree burns aren't a personality trait.", 0xFFFF88AA.toInt()),
            ChatMessage("EMBER", "They are if you're committed enough to the flame.", 0xFFFF6622.toInt()),
            ChatMessage("MEDIC", "I'm going to run out of burn cream at this rate.", 0xFFFF88AA.toInt()),
            ChatMessage("EMBER", "The flame demands sacrifice.", 0xFFFF6622.toInt())
        )),
        BarConversation("pilot_medic", "pilot_ember", listOf(
            ChatMessage("EMBER", "Pain is merely the fire testing your resolve.", 0xFFFF6622.toInt()),
            ChatMessage("MEDIC", "Pain is your body politely saying stop.", 0xFFFF88AA.toInt()),
            ChatMessage("EMBER", "My body says burn brighter.", 0xFFFF6622.toInt()),
            ChatMessage("MEDIC", "Your body is wrong.", 0xFFFF88AA.toInt())
        )),
        BarConversation("pilot_medic", "pilot_ember", listOf(
            ChatMessage("MEDIC", "Ember, please stop cauterizing your own wounds.", 0xFFFF88AA.toInt()),
            ChatMessage("EMBER", "It's efficient!", 0xFFFF6622.toInt()),
            ChatMessage("MEDIC", "It's terrifying.", 0xFFFF88AA.toInt())
        )),

        // ============================================================
        // === MEDIC + FANG ===
        // ============================================================
        BarConversation("pilot_medic", "pilot_fang", listOf(
            ChatMessage("FANG", "I can hear your heartbeat from across the room.", 0xFF8844CC.toInt()),
            ChatMessage("MEDIC", "That's... medically impressive, actually.", 0xFFFF88AA.toInt()),
            ChatMessage("FANG", "It speeds up when I talk. Interesting.", 0xFF8844CC.toInt()),
            ChatMessage("MEDIC", "That's called discomfort, Fang.", 0xFFFF88AA.toInt())
        )),
        BarConversation("pilot_medic", "pilot_fang", listOf(
            ChatMessage("MEDIC", "Fang, when did you last eat something normal?", 0xFFFF88AA.toInt()),
            ChatMessage("FANG", "Define normal.", 0xFF8844CC.toInt()),
            ChatMessage("MEDIC", "Something that wasn't still raw, Fang.", 0xFFFF88AA.toInt()),
            ChatMessage("FANG", "I'll think about it.", 0xFF8844CC.toInt())
        )),
        BarConversation("pilot_medic", "pilot_fang", listOf(
            ChatMessage("FANG", "You always smell like antiseptic.", 0xFF8844CC.toInt()),
            ChatMessage("MEDIC", "You smell like a cave.", 0xFFFF88AA.toInt()),
            ChatMessage("FANG", "Thank you.", 0xFF8844CC.toInt())
        )),

        // ============================================================
        // === MEDIC + WHISKERS ===
        // ============================================================
        BarConversation("pilot_medic", "pilot_whiskers", listOf(
            ChatMessage("MEDIC", "Whiskers, you missed your annual checkup.", 0xFFFF88AA.toInt()),
            ChatMessage("WHISKERS", "I didn't miss it. I ignored it.", 0xFFFFBB88.toInt()),
            ChatMessage("MEDIC", "That's worse.", 0xFFFF88AA.toInt()),
            ChatMessage("WHISKERS", "For you, maybe.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_medic", "pilot_whiskers", listOf(
            ChatMessage("WHISKERS", "I have a hairball. Do something about it.", 0xFFFFBB88.toInt()),
            ChatMessage("MEDIC", "That's not really my medical speciality.", 0xFFFF88AA.toInt()),
            ChatMessage("WHISKERS", "You're a doctor. Doctor it.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_medic", "pilot_whiskers", listOf(
            ChatMessage("MEDIC", "Your reflexes tested off the charts.", 0xFFFF88AA.toInt()),
            ChatMessage("WHISKERS", "Obviously.", 0xFFFFBB88.toInt()),
            ChatMessage("MEDIC", "It wasn't a compliment. You scratched the nurse.", 0xFFFF88AA.toInt()),
            ChatMessage("WHISKERS", "She touched my belly.", 0xFFFFBB88.toInt())
        )),

        // ============================================================
        // === MEDIC + KRAKEN ===
        // ============================================================
        BarConversation("pilot_medic", "pilot_kraken", listOf(
            ChatMessage("MEDIC", "Kraken, I'm going to need a blood sample.", 0xFFFF88AA.toInt()),
            ChatMessage("KRAKEN", "Which arm?", 0xFF33AAAA.toInt()),
            ChatMessage("MEDIC", "Any of the eight will do, thank you.", 0xFFFF88AA.toInt()),
            ChatMessage("KRAKEN", "Choose wisely. Some bite.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_medic", "pilot_kraken", listOf(
            ChatMessage("KRAKEN", "The body is a vessel. The mind is the ocean.", 0xFF33AAAA.toInt()),
            ChatMessage("MEDIC", "The body is full of organs that I keep fixing.", 0xFFFF88AA.toInt()),
            ChatMessage("KRAKEN", "A pragmatic perspective. Refreshing.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_medic", "pilot_kraken", listOf(
            ChatMessage("MEDIC", "Your blood pressure just broke my second cuff.", 0xFFFF88AA.toInt()),
            ChatMessage("KRAKEN", "I have three hearts.", 0xFF33AAAA.toInt()),
            ChatMessage("MEDIC", "That would explain the chart.", 0xFFFF88AA.toInt()),
            ChatMessage("KRAKEN", "And the emotional depth.", 0xFF33AAAA.toInt())
        )),

        // ============================================================
        // === MEDIC + HAVOC ===
        // ============================================================
        BarConversation("pilot_medic", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "MEDIC! I'm hurt! Just kidding. Check this bruise though.", 0xFFBBFF22.toInt()),
            ChatMessage("MEDIC", "That's a serious contusion. How?", 0xFFFF88AA.toInt()),
            ChatMessage("HAVOC", "Headbutted a bulkhead. For fun, mostly.", 0xFFBBFF22.toInt()),
            ChatMessage("MEDIC", "I'm going to start charging you extra.", 0xFFFF88AA.toInt())
        )),
        BarConversation("pilot_medic", "pilot_havoc", listOf(
            ChatMessage("MEDIC", "You've visited the med-bay seven times. This week.", 0xFFFF88AA.toInt()),
            ChatMessage("HAVOC", "New record! YES!", 0xFFBBFF22.toInt()),
            ChatMessage("MEDIC", "That's not something to celebrate.", 0xFFFF88AA.toInt())
        )),
        BarConversation("pilot_medic", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "What's the worst injury you've ever seen?", 0xFFBBFF22.toInt()),
            ChatMessage("MEDIC", "I'm looking at him.", 0xFFFF88AA.toInt()),
            ChatMessage("HAVOC", "AWESOME!", 0xFFBBFF22.toInt()),
            ChatMessage("MEDIC", "Still not a compliment.", 0xFFFF88AA.toInt())
        )),

        // ============================================================
        // === MEDIC + UNIT-7 ===
        // ============================================================
        BarConversation("pilot_medic", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Query: why do organics require sleep?", 0xFF44EE55.toInt()),
            ChatMessage("MEDIC", "Our brains need downtime to process the day.", 0xFFFF88AA.toInt()),
            ChatMessage("UNIT-7", "Inefficient. I defragment in 0.3 seconds.", 0xFF44EE55.toInt()),
            ChatMessage("MEDIC", "Must be nice.", 0xFFFF88AA.toInt())
        )),
        BarConversation("pilot_medic", "pilot_unit7", listOf(
            ChatMessage("MEDIC", "Unit-7, I can't give you a physical. You're metal.", 0xFFFF88AA.toInt()),
            ChatMessage("UNIT-7", "Request: perform diagnostic on chassis integrity.", 0xFF44EE55.toInt()),
            ChatMessage("MEDIC", "That's engineering, not medicine.", 0xFFFF88AA.toInt()),
            ChatMessage("UNIT-7", "The distinction is unclear.", 0xFF44EE55.toInt())
        )),
        BarConversation("pilot_medic", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Your heart rate elevated 12% when Astro entered.", 0xFF44EE55.toInt()),
            ChatMessage("MEDIC", "That's-- I was, uh, exercising.", 0xFFFF88AA.toInt()),
            ChatMessage("UNIT-7", "You were sitting down.", 0xFF44EE55.toInt())
        )),

        // ============================================================
        // === MEDIC + ASTRO (subtle flirtation) ===
        // ============================================================
        BarConversation("pilot_medic", "pilot_astro", listOf(
            ChatMessage("MEDIC", "You should let me check your vitals sometime.", 0xFFFF88AA.toInt()),
            ChatMessage("ASTRO", "Is that a strictly medical recommendation?", 0xFFDD3333.toInt()),
            ChatMessage("MEDIC", "Purely professional. Mostly.", 0xFFFF88AA.toInt()),
            ChatMessage("ASTRO", "I'll, uh... I'll pencil that in.", 0xFFDD3333.toInt())
        )),
        BarConversation("pilot_medic", "pilot_astro", listOf(
            ChatMessage("ASTRO", "That new flight suit looks good on you.", 0xFFDD3333.toInt()),
            ChatMessage("MEDIC", "These are just medical scrubs, Astro.", 0xFFFF88AA.toInt()),
            ChatMessage("ASTRO", "Still looks good.", 0xFFDD3333.toInt()),
            ChatMessage("MEDIC", "...thanks. Shut up.", 0xFFFF88AA.toInt())
        )),
        BarConversation("pilot_medic", "pilot_astro", listOf(
            ChatMessage("MEDIC", "You got hit out there. Sit down, let me see.", 0xFFFF88AA.toInt()),
            ChatMessage("ASTRO", "It's just a scratch.", 0xFFDD3333.toInt()),
            ChatMessage("MEDIC", "I'll decide what's a scratch. Hold still.", 0xFFFF88AA.toInt()),
            ChatMessage("ASTRO", "Yes ma'am.", 0xFFDD3333.toInt())
        )),

        // ============================================================
        // === RASCAL + BRUTUS ===
        // ============================================================
        BarConversation("pilot_rascal", "pilot_brutus", listOf(
            ChatMessage("BRUTUS", "Where's my wrench.", 0xFF77AA33.toInt()),
            ChatMessage("RASCAL", "No idea! That's so weird! Anyway, gotta go!", 0xFFDDAA33.toInt()),
            ChatMessage("BRUTUS", "Rascal.", 0xFF77AA33.toInt()),
            ChatMessage("RASCAL", "...it's behind the crate. Please don't hurt me.", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_brutus", listOf(
            ChatMessage("RASCAL", "Hey Brutus, you want half of this sandwich?", 0xFFDDAA33.toInt()),
            ChatMessage("BRUTUS", "Whose sandwich.", 0xFF77AA33.toInt()),
            ChatMessage("RASCAL", "Does it matter?", 0xFFDDAA33.toInt()),
            ChatMessage("BRUTUS", "...give it.", 0xFF77AA33.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_brutus", listOf(
            ChatMessage("RASCAL", "You're huge. Like a warm fuzzy mountain.", 0xFFDDAA33.toInt()),
            ChatMessage("BRUTUS", "Don't touch.", 0xFF77AA33.toInt()),
            ChatMessage("RASCAL", "I bet your fur is super soft though.", 0xFFDDAA33.toInt())
        )),

        // ============================================================
        // === RASCAL + FROST ===
        // ============================================================
        BarConversation("pilot_rascal", "pilot_frost", listOf(
            ChatMessage("RASCAL", "Hey Frost, what're you keeping in that locker?", 0xFFDDAA33.toInt()),
            ChatMessage("FROST", "A lock. Which you've already picked twice.", 0xFF55BBFF.toInt()),
            ChatMessage("RASCAL", "Third time's the charm!", 0xFFDDAA33.toInt()),
            ChatMessage("FROST", "I installed a cryo-trap. Good luck.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_frost", listOf(
            ChatMessage("FROST", "You ate my entire fish reserve, Rascal.", 0xFF55BBFF.toInt()),
            ChatMessage("RASCAL", "They smelled too good to leave alone.", 0xFFDDAA33.toInt()),
            ChatMessage("FROST", "Those were flash-frozen sardines for calibration.", 0xFF55BBFF.toInt()),
            ChatMessage("RASCAL", "Calibration sardines taste the best.", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_frost", listOf(
            ChatMessage("RASCAL", "You never get mad. It's a little unsettling.", 0xFFDDAA33.toInt()),
            ChatMessage("FROST", "I get cold. Colder than usual.", 0xFF55BBFF.toInt()),
            ChatMessage("RASCAL", "Is that a threat?", 0xFFDDAA33.toInt()),
            ChatMessage("FROST", "It's a forecast.", 0xFF55BBFF.toInt())
        )),

        // ============================================================
        // === RASCAL + DASH ===
        // ============================================================
        BarConversation("pilot_rascal", "pilot_dash", listOf(
            ChatMessage("RASCAL", "Bet I can steal your badge before you blink.", 0xFFDDAA33.toInt()),
            ChatMessage("DASH", "Bet I can catch you before you even run.", 0xFFFFDD22.toInt()),
            ChatMessage("RASCAL", "Deal.", 0xFFDDAA33.toInt()),
            ChatMessage("DASH", "Already got it back. You're slow.", 0xFFFFDD22.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_dash", listOf(
            ChatMessage("DASH", "Rascal! Come race me around the whole hangar!", 0xFFFFDD22.toInt()),
            ChatMessage("RASCAL", "I don't race. I sneak.", 0xFFDDAA33.toInt()),
            ChatMessage("DASH", "Sneaking is just slow racing.", 0xFFFFDD22.toInt()),
            ChatMessage("RASCAL", "Racing is just loud sneaking.", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_dash", listOf(
            ChatMessage("RASCAL", "Slow down, will you? You're making me dizzy.", 0xFFDDAA33.toInt()),
            ChatMessage("DASH", "You look dizzy all the time.", 0xFFFFDD22.toInt()),
            ChatMessage("RASCAL", "That's the vent fumes.", 0xFFDDAA33.toInt())
        )),

        // ============================================================
        // === RASCAL + EMBER ===
        // ============================================================
        BarConversation("pilot_rascal", "pilot_ember", listOf(
            ChatMessage("RASCAL", "Ember! Your tail just singed my whole stash!", 0xFFDDAA33.toInt()),
            ChatMessage("EMBER", "You shouldn't hide things near a phoenix.", 0xFFFF6622.toInt()),
            ChatMessage("RASCAL", "I had good crackers in there!", 0xFFDDAA33.toInt()),
            ChatMessage("EMBER", "Now you have croutons.", 0xFFFF6622.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_ember", listOf(
            ChatMessage("EMBER", "From the ashes, we are reborn and rise.", 0xFFFF6622.toInt()),
            ChatMessage("RASCAL", "From the dumpster, I rise.", 0xFFDDAA33.toInt()),
            ChatMessage("EMBER", "That's... not the same thing.", 0xFFFF6622.toInt()),
            ChatMessage("RASCAL", "Same energy though.", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_ember", listOf(
            ChatMessage("RASCAL", "Can you heat up this burrito for me?", 0xFFDDAA33.toInt()),
            ChatMessage("EMBER", "I am not a microwave.", 0xFFFF6622.toInt()),
            ChatMessage("RASCAL", "Pretty please?", 0xFFDDAA33.toInt()),
            ChatMessage("EMBER", "...give it here.", 0xFFFF6622.toInt())
        )),

        // ============================================================
        // === RASCAL + FANG ===
        // ============================================================
        BarConversation("pilot_rascal", "pilot_fang", listOf(
            ChatMessage("FANG", "I can see you perfectly in the dark, Rascal.", 0xFF8844CC.toInt()),
            ChatMessage("RASCAL", "That's cheating!", 0xFFDDAA33.toInt()),
            ChatMessage("FANG", "It's called echolocation, little thief.", 0xFF8844CC.toInt()),
            ChatMessage("RASCAL", "I call it unfair.", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_fang", listOf(
            ChatMessage("RASCAL", "Fang, you awake?", 0xFFDDAA33.toInt()),
            ChatMessage("FANG", "It's 3 AM. Of course I'm wide awake.", 0xFF8844CC.toInt()),
            ChatMessage("RASCAL", "You want to go raid the pantry with me?", 0xFFDDAA33.toInt()),
            ChatMessage("FANG", "...lead the way.", 0xFF8844CC.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_fang", listOf(
            ChatMessage("RASCAL", "Why do you always hang upside down like that?", 0xFFDDAA33.toInt()),
            ChatMessage("FANG", "Better perspective.", 0xFF8844CC.toInt()),
            ChatMessage("RASCAL", "Of what?", 0xFFDDAA33.toInt()),
            ChatMessage("FANG", "Everything you're trying to steal.", 0xFF8844CC.toInt())
        )),

        // ============================================================
        // === RASCAL + WHISKERS ===
        // ============================================================
        BarConversation("pilot_rascal", "pilot_whiskers", listOf(
            ChatMessage("RASCAL", "Hey Whiskers, you wanna play a game?", 0xFFDDAA33.toInt()),
            ChatMessage("WHISKERS", "No.", 0xFFFFBB88.toInt()),
            ChatMessage("RASCAL", "I found a working laser pointer!", 0xFFDDAA33.toInt()),
            ChatMessage("WHISKERS", "...where is it.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_whiskers", listOf(
            ChatMessage("WHISKERS", "You touched my tail.", 0xFFFFBB88.toInt()),
            ChatMessage("RASCAL", "It was so fluffy! I couldn't help myself!", 0xFFDDAA33.toInt()),
            ChatMessage("WHISKERS", "Touch it again and you lose a finger.", 0xFFFFBB88.toInt()),
            ChatMessage("RASCAL", "Worth it.", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_whiskers", listOf(
            ChatMessage("RASCAL", "We're both small and sneaky. We should team up.", 0xFFDDAA33.toInt()),
            ChatMessage("WHISKERS", "I am neither small nor sneaky. I am elegant.", 0xFFFFBB88.toInt()),
            ChatMessage("RASCAL", "Elegantly sneaky.", 0xFFDDAA33.toInt()),
            ChatMessage("WHISKERS", "Get away from me.", 0xFFFFBB88.toInt())
        )),

        // ============================================================
        // === RASCAL + KRAKEN ===
        // ============================================================
        BarConversation("pilot_rascal", "pilot_kraken", listOf(
            ChatMessage("RASCAL", "How do you even pick pockets with tentacles?", 0xFFDDAA33.toInt()),
            ChatMessage("KRAKEN", "I don't pick pockets. I have dignity.", 0xFF33AAAA.toInt()),
            ChatMessage("RASCAL", "But hypothetically...", 0xFFDDAA33.toInt()),
            ChatMessage("KRAKEN", "Hypothetically? All eight at once.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_kraken", listOf(
            ChatMessage("KRAKEN", "You remind me of the cleaner fish. Small. Bold. Useful.", 0xFF33AAAA.toInt()),
            ChatMessage("RASCAL", "Was that a compliment?", 0xFFDDAA33.toInt()),
            ChatMessage("KRAKEN", "It was an observation.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_kraken", listOf(
            ChatMessage("RASCAL", "Hey Kraken, can I borrow one of your tentacles?", 0xFFDDAA33.toInt()),
            ChatMessage("KRAKEN", "They don't detach.", 0xFF33AAAA.toInt()),
            ChatMessage("RASCAL", "That's quitter talk.", 0xFFDDAA33.toInt()),
            ChatMessage("KRAKEN", "That's biology.", 0xFF33AAAA.toInt())
        )),

        // ============================================================
        // === RASCAL + HAVOC ===
        // ============================================================
        BarConversation("pilot_rascal", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "Rascal! Come on, let's go blow something up!", 0xFFBBFF22.toInt()),
            ChatMessage("RASCAL", "I'm more a take-things guy than a break-things guy.", 0xFFDDAA33.toInt()),
            ChatMessage("HAVOC", "So steal the explosives! Then we both win!", 0xFFBBFF22.toInt()),
            ChatMessage("RASCAL", "...that's actually not bad.", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_havoc", listOf(
            ChatMessage("RASCAL", "You're loud enough to cover all my exits.", 0xFFDDAA33.toInt()),
            ChatMessage("HAVOC", "I'M ALWAYS LOUD!", 0xFFBBFF22.toInt()),
            ChatMessage("RASCAL", "That's what I'm counting on.", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "I found your little stash up in vent seven.", 0xFFBBFF22.toInt()),
            ChatMessage("RASCAL", "You WHAT?!", 0xFFDDAA33.toInt()),
            ChatMessage("HAVOC", "Relax, relax, I only ate the jerky.", 0xFFBBFF22.toInt()),
            ChatMessage("RASCAL", "THAT WAS VINTAGE JERKY!", 0xFFDDAA33.toInt())
        )),

        // ============================================================
        // === RASCAL + UNIT-7 ===
        // ============================================================
        BarConversation("pilot_rascal", "pilot_unit7", listOf(
            ChatMessage("RASCAL", "Hey Unit-7, you got any loose screws on you?", 0xFFDDAA33.toInt()),
            ChatMessage("UNIT-7", "All my screws are torqued to specification.", 0xFF44EE55.toInt()),
            ChatMessage("RASCAL", "Shame. Those would fetch good yen.", 0xFFDDAA33.toInt()),
            ChatMessage("UNIT-7", "Clarification: are you attempting theft?", 0xFF44EE55.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "You have taken 14 items that do not belong to you today.", 0xFF44EE55.toInt()),
            ChatMessage("RASCAL", "Only 14? I'm slipping.", 0xFFDDAA33.toInt()),
            ChatMessage("UNIT-7", "This behavior is classified as theft.", 0xFF44EE55.toInt()),
            ChatMessage("RASCAL", "I prefer 'redistribution.'", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_unit7", listOf(
            ChatMessage("RASCAL", "Do robots dream?", 0xFFDDAA33.toInt()),
            ChatMessage("UNIT-7", "I enter a low-power diagnostic mode.", 0xFF44EE55.toInt()),
            ChatMessage("RASCAL", "So that's a no.", 0xFFDDAA33.toInt()),
            ChatMessage("UNIT-7", "I dreamed once. It was a firmware error.", 0xFF44EE55.toInt())
        )),

        // ============================================================
        // === RASCAL + ASTRO ===
        // ============================================================
        BarConversation("pilot_rascal", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Rascal. Return whatever it is you took.", 0xFFDD3333.toInt()),
            ChatMessage("RASCAL", "I didn't take anything!", 0xFFDDAA33.toInt()),
            ChatMessage("ASTRO", "Rascal.", 0xFFDD3333.toInt()),
            ChatMessage("RASCAL", "...it's in vent four.", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_astro", listOf(
            ChatMessage("RASCAL", "Boss, can I get a bigger cut of the yen?", 0xFFDDAA33.toInt()),
            ChatMessage("ASTRO", "You already take a generous unofficial cut.", 0xFFDD3333.toInt()),
            ChatMessage("RASCAL", "I want an official one too.", 0xFFDDAA33.toInt())
        )),
        BarConversation("pilot_rascal", "pilot_astro", listOf(
            ChatMessage("ASTRO", "That was good flying today, Rascal.", 0xFFDD3333.toInt()),
            ChatMessage("RASCAL", "Thanks boss! I also found this in your cockpit.", 0xFFDDAA33.toInt()),
            ChatMessage("ASTRO", "That's my flask. You stole my flask.", 0xFFDD3333.toInt()),
            ChatMessage("RASCAL", "I said I found it!", 0xFFDDAA33.toInt())
        )),

        // ============================================================
        // === BRUTUS + FROST ===
        // ============================================================
        BarConversation("pilot_brutus", "pilot_frost", listOf(
            ChatMessage("FROST", "Your raw combat efficiency is admirable.", 0xFF55BBFF.toInt()),
            ChatMessage("BRUTUS", "Hit things. They break.", 0xFF77AA33.toInt()),
            ChatMessage("FROST", "Elegantly simple.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_frost", listOf(
            ChatMessage("BRUTUS", "Cold.", 0xFF77AA33.toInt()),
            ChatMessage("FROST", "You're welcome.", 0xFF55BBFF.toInt()),
            ChatMessage("BRUTUS", "Wasn't a compliment.", 0xFF77AA33.toInt()),
            ChatMessage("FROST", "Wasn't an apology.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_frost", listOf(
            ChatMessage("FROST", "Have you considered strategic retreat?", 0xFF55BBFF.toInt()),
            ChatMessage("BRUTUS", "No.", 0xFF77AA33.toInt()),
            ChatMessage("FROST", "Noted. Moving on.", 0xFF55BBFF.toInt())
        )),

        // ============================================================
        // === BRUTUS + DASH ===
        // ============================================================
        BarConversation("pilot_brutus", "pilot_dash", listOf(
            ChatMessage("DASH", "Brutus! Brutus! Come on, race me already!", 0xFFFFDD22.toInt()),
            ChatMessage("BRUTUS", "No.", 0xFF77AA33.toInt()),
            ChatMessage("DASH", "What if I give you a head start?", 0xFFFFDD22.toInt()),
            ChatMessage("BRUTUS", "Still no.", 0xFF77AA33.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_dash", listOf(
            ChatMessage("DASH", "How do you move that slow and still win fights?", 0xFFFFDD22.toInt()),
            ChatMessage("BRUTUS", "Don't need speed. Need strength.", 0xFF77AA33.toInt()),
            ChatMessage("DASH", "That's... actually kind of cool.", 0xFFFFDD22.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_dash", listOf(
            ChatMessage("DASH", "I lapped you five whole times during drills.", 0xFFFFDD22.toInt()),
            ChatMessage("BRUTUS", "I finished once. That was enough.", 0xFF77AA33.toInt()),
            ChatMessage("DASH", "But don't you want to go faster?", 0xFFFFDD22.toInt()),
            ChatMessage("BRUTUS", "Why.", 0xFF77AA33.toInt())
        )),

        // ============================================================
        // === BRUTUS + EMBER ===
        // ============================================================
        BarConversation("pilot_brutus", "pilot_ember", listOf(
            ChatMessage("EMBER", "The fire that burns within me is eternal!", 0xFFFF6622.toInt()),
            ChatMessage("BRUTUS", "Loud.", 0xFF77AA33.toInt()),
            ChatMessage("EMBER", "Art requires volume!", 0xFFFF6622.toInt()),
            ChatMessage("BRUTUS", "Art requires silence.", 0xFF77AA33.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_ember", listOf(
            ChatMessage("BRUTUS", "You singed my fur.", 0xFF77AA33.toInt()),
            ChatMessage("EMBER", "The flame reaches where it must.", 0xFFFF6622.toInt()),
            ChatMessage("BRUTUS", "Next time it reaches, I snap it off.", 0xFF77AA33.toInt()),
            ChatMessage("EMBER", "...I'll be more careful.", 0xFFFF6622.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_ember", listOf(
            ChatMessage("EMBER", "We are both forces of nature, Brutus.", 0xFFFF6622.toInt()),
            ChatMessage("BRUTUS", "You talk too much.", 0xFF77AA33.toInt()),
            ChatMessage("EMBER", "Fire speaks in actions.", 0xFFFF6622.toInt()),
            ChatMessage("BRUTUS", "Good. Start now.", 0xFF77AA33.toInt())
        )),

        // ============================================================
        // === BRUTUS + FANG ===
        // ============================================================
        BarConversation("pilot_brutus", "pilot_fang", listOf(
            ChatMessage("FANG", "You move surprisingly quiet for something that big.", 0xFF8844CC.toInt()),
            ChatMessage("BRUTUS", "Quiet is good.", 0xFF77AA33.toInt()),
            ChatMessage("FANG", "We agree on something.", 0xFF8844CC.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_fang", listOf(
            ChatMessage("BRUTUS", "Stop staring.", 0xFF77AA33.toInt()),
            ChatMessage("FANG", "I wasn't staring. I was observing.", 0xFF8844CC.toInt()),
            ChatMessage("BRUTUS", "Same thing.", 0xFF77AA33.toInt()),
            ChatMessage("FANG", "Not from the shadows.", 0xFF8844CC.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_fang", listOf(
            ChatMessage("FANG", "Night shift together?", 0xFF8844CC.toInt()),
            ChatMessage("BRUTUS", "Fine.", 0xFF77AA33.toInt()),
            ChatMessage("FANG", "You don't talk much. I appreciate that.", 0xFF8844CC.toInt()),
            ChatMessage("BRUTUS", "Hmph.", 0xFF77AA33.toInt())
        )),

        // ============================================================
        // === BRUTUS + WHISKERS ===
        // ============================================================
        BarConversation("pilot_brutus", "pilot_whiskers", listOf(
            ChatMessage("WHISKERS", "Move. That's my seat.", 0xFFFFBB88.toInt()),
            ChatMessage("BRUTUS", "No.", 0xFF77AA33.toInt()),
            ChatMessage("WHISKERS", "I was sitting there first, you know.", 0xFFFFBB88.toInt()),
            ChatMessage("BRUTUS", "Prove it.", 0xFF77AA33.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_whiskers", listOf(
            ChatMessage("WHISKERS", "Your shedding is worse than mine.", 0xFFFFBB88.toInt()),
            ChatMessage("BRUTUS", "Bears don't shed.", 0xFF77AA33.toInt()),
            ChatMessage("WHISKERS", "Then explain the fur on my cushion.", 0xFFFFBB88.toInt()),
            ChatMessage("BRUTUS", "...no.", 0xFF77AA33.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_whiskers", listOf(
            ChatMessage("BRUTUS", "Cat.", 0xFF77AA33.toInt()),
            ChatMessage("WHISKERS", "Bear.", 0xFFFFBB88.toInt()),
            ChatMessage("BRUTUS", "Hmph.", 0xFF77AA33.toInt()),
            ChatMessage("WHISKERS", "Indeed.", 0xFFFFBB88.toInt())
        )),

        // ============================================================
        // === BRUTUS + KRAKEN ===
        // ============================================================
        BarConversation("pilot_brutus", "pilot_kraken", listOf(
            ChatMessage("KRAKEN", "Strength without wisdom is a storm without direction.", 0xFF33AAAA.toInt()),
            ChatMessage("BRUTUS", "Storms don't need direction.", 0xFF77AA33.toInt()),
            ChatMessage("KRAKEN", "A fair counter-argument.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_kraken", listOf(
            ChatMessage("BRUTUS", "Arm wrestle.", 0xFF77AA33.toInt()),
            ChatMessage("KRAKEN", "Which arm?", 0xFF33AAAA.toInt()),
            ChatMessage("BRUTUS", "All of them.", 0xFF77AA33.toInt()),
            ChatMessage("KRAKEN", "Respect.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_kraken", listOf(
            ChatMessage("KRAKEN", "The deep ocean taught me patience.", 0xFF33AAAA.toInt()),
            ChatMessage("BRUTUS", "Combat taught me not to wait.", 0xFF77AA33.toInt()),
            ChatMessage("KRAKEN", "Both lessons have value.", 0xFF33AAAA.toInt()),
            ChatMessage("BRUTUS", "Mine's faster.", 0xFF77AA33.toInt())
        )),

        // ============================================================
        // === BRUTUS + HAVOC ===
        // ============================================================
        BarConversation("pilot_brutus", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "Hey Brutus! You wanna see something cool?", 0xFFBBFF22.toInt()),
            ChatMessage("BRUTUS", "No.", 0xFF77AA33.toInt()),
            ChatMessage("HAVOC", "TOO LATE!", 0xFFBBFF22.toInt()),
            ChatMessage("BRUTUS", "...I hate this.", 0xFF77AA33.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "BRUTUS! HIGH FIVE!", 0xFFBBFF22.toInt()),
            ChatMessage("BRUTUS", "No.", 0xFF77AA33.toInt()),
            ChatMessage("HAVOC", "Come on! Don't leave me hanging!", 0xFFBBFF22.toInt()),
            ChatMessage("BRUTUS", "Leaving you hanging.", 0xFF77AA33.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "We should spar! Full contact! No rules!", 0xFFBBFF22.toInt()),
            ChatMessage("BRUTUS", "You'd break.", 0xFF77AA33.toInt()),
            ChatMessage("HAVOC", "THAT'S THE SPIRIT!", 0xFFBBFF22.toInt())
        )),

        // ============================================================
        // === BRUTUS + UNIT-7 ===
        // ============================================================
        BarConversation("pilot_brutus", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Query: why do you growl so frequently?", 0xFF44EE55.toInt()),
            ChatMessage("BRUTUS", "Warning.", 0xFF77AA33.toInt()),
            ChatMessage("UNIT-7", "Warning of what?", 0xFF44EE55.toInt()),
            ChatMessage("BRUTUS", "This conversation.", 0xFF77AA33.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Your bone density is remarkable.", 0xFF44EE55.toInt()),
            ChatMessage("BRUTUS", "Stop scanning me.", 0xFF77AA33.toInt()),
            ChatMessage("UNIT-7", "It is involuntary. Your mass is notable.", 0xFF44EE55.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_unit7", listOf(
            ChatMessage("BRUTUS", "Metal.", 0xFF77AA33.toInt()),
            ChatMessage("UNIT-7", "Affirmative. Titanium alloy.", 0xFF44EE55.toInt()),
            ChatMessage("BRUTUS", "Hard?", 0xFF77AA33.toInt()),
            ChatMessage("UNIT-7", "Extremely.", 0xFF44EE55.toInt())
        )),

        // ============================================================
        // === BRUTUS + ASTRO ===
        // ============================================================
        BarConversation("pilot_brutus", "pilot_astro", listOf(
            ChatMessage("ASTRO", "That was good work out there, Brutus.", 0xFFDD3333.toInt()),
            ChatMessage("BRUTUS", "Hmph.", 0xFF77AA33.toInt()),
            ChatMessage("ASTRO", "That's a thank you, right?", 0xFFDD3333.toInt()),
            ChatMessage("BRUTUS", "Close enough.", 0xFF77AA33.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Need anything, big guy?", 0xFFDD3333.toInt()),
            ChatMessage("BRUTUS", "Quiet.", 0xFF77AA33.toInt()),
            ChatMessage("ASTRO", "Fair enough.", 0xFFDD3333.toInt())
        )),
        BarConversation("pilot_brutus", "pilot_astro", listOf(
            ChatMessage("BRUTUS", "Astro.", 0xFF77AA33.toInt()),
            ChatMessage("ASTRO", "Brutus. Something on your mind?", 0xFFDD3333.toInt()),
            ChatMessage("BRUTUS", "No. Just checking in.", 0xFF77AA33.toInt()),
            ChatMessage("ASTRO", "Appreciate it.", 0xFFDD3333.toInt())
        )),

        // ============================================================
        // === FROST + DASH ===
        // ============================================================
        BarConversation("pilot_frost", "pilot_dash", listOf(
            ChatMessage("DASH", "Frost! Frost! Come on, guess my lap time!", 0xFFFFDD22.toInt()),
            ChatMessage("FROST", "3.7 seconds. I was counting.", 0xFF55BBFF.toInt()),
            ChatMessage("DASH", "HOW DID YOU KNOW?!", 0xFFFFDD22.toInt()),
            ChatMessage("FROST", "Math. Try it sometime.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_frost", "pilot_dash", listOf(
            ChatMessage("FROST", "You're making me dizzy. Stop circling the table.", 0xFF55BBFF.toInt()),
            ChatMessage("DASH", "I can't stop! Momentum!", 0xFFFFDD22.toInt()),
            ChatMessage("FROST", "That's not how momentum works indoors.", 0xFF55BBFF.toInt()),
            ChatMessage("DASH", "INDOOR MOMENTUM!", 0xFFFFDD22.toInt())
        )),
        BarConversation("pilot_frost", "pilot_dash", listOf(
            ChatMessage("DASH", "Don't you ever want to go fast?", 0xFFFFDD22.toInt()),
            ChatMessage("FROST", "Penguins are fast. Underwater.", 0xFF55BBFF.toInt()),
            ChatMessage("DASH", "That doesn't count!", 0xFFFFDD22.toInt()),
            ChatMessage("FROST", "It counts to the fish.", 0xFF55BBFF.toInt())
        )),

        // ============================================================
        // === FROST + EMBER ===
        // ============================================================
        BarConversation("pilot_frost", "pilot_ember", listOf(
            ChatMessage("FROST", "You're raising the ambient temperature again.", 0xFF55BBFF.toInt()),
            ChatMessage("EMBER", "And you're lowering it. We balance out.", 0xFFFF6622.toInt()),
            ChatMessage("FROST", "That's not how thermodynamics works.", 0xFF55BBFF.toInt()),
            ChatMessage("EMBER", "Live a little, Frost.", 0xFFFF6622.toInt())
        )),
        BarConversation("pilot_frost", "pilot_ember", listOf(
            ChatMessage("EMBER", "Ice is just fire that gave up.", 0xFFFF6622.toInt()),
            ChatMessage("FROST", "Ice is patient. Fire is wasteful.", 0xFF55BBFF.toInt()),
            ChatMessage("EMBER", "Wasteful?! I am RADIANT.", 0xFFFF6622.toInt()),
            ChatMessage("FROST", "You're room temperature. Soon.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_frost", "pilot_ember", listOf(
            ChatMessage("FROST", "Stay on your own side of the bar, Ember.", 0xFF55BBFF.toInt()),
            ChatMessage("EMBER", "The flame goes where the flame pleases.", 0xFFFF6622.toInt()),
            ChatMessage("FROST", "The frost goes where the flame regrets.", 0xFF55BBFF.toInt()),
            ChatMessage("EMBER", "...fair.", 0xFFFF6622.toInt())
        )),

        // ============================================================
        // === FROST + FANG ===
        // ============================================================
        BarConversation("pilot_frost", "pilot_fang", listOf(
            ChatMessage("FANG", "Cold and dark. We have much in common.", 0xFF8844CC.toInt()),
            ChatMessage("FROST", "I'm cold. You're creepy. Slight difference.", 0xFF55BBFF.toInt()),
            ChatMessage("FANG", "Creepy is subjective.", 0xFF8844CC.toInt()),
            ChatMessage("FROST", "Objectively creepy.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_frost", "pilot_fang", listOf(
            ChatMessage("FROST", "Antarctic nights can last a full six months.", 0xFF55BBFF.toInt()),
            ChatMessage("FANG", "Paradise.", 0xFF8844CC.toInt()),
            ChatMessage("FROST", "That was actually meant as a complaint.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_frost", "pilot_fang", listOf(
            ChatMessage("FANG", "Your blood runs cold.", 0xFF8844CC.toInt()),
            ChatMessage("FROST", "Technically warm-blooded. Just very well-insulated.", 0xFF55BBFF.toInt()),
            ChatMessage("FANG", "I was being poetic.", 0xFF8844CC.toInt()),
            ChatMessage("FROST", "I was being accurate.", 0xFF55BBFF.toInt())
        )),

        // ============================================================
        // === FROST + WHISKERS ===
        // ============================================================
        BarConversation("pilot_frost", "pilot_whiskers", listOf(
            ChatMessage("WHISKERS", "You waddle.", 0xFFFFBB88.toInt()),
            ChatMessage("FROST", "You knock things off tables on purpose.", 0xFF55BBFF.toInt()),
            ChatMessage("WHISKERS", "Touche.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_frost", "pilot_whiskers", listOf(
            ChatMessage("FROST", "Your napping habits are statistically impressive.", 0xFF55BBFF.toInt()),
            ChatMessage("WHISKERS", "Sixteen hours minimum.", 0xFFFFBB88.toInt()),
            ChatMessage("FROST", "I meant impressively wasteful.", 0xFF55BBFF.toInt()),
            ChatMessage("WHISKERS", "Jealousy is unbecoming.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_frost", "pilot_whiskers", listOf(
            ChatMessage("WHISKERS", "Fish breath.", 0xFFFFBB88.toInt()),
            ChatMessage("FROST", "You eat yours straight from a tin.", 0xFF55BBFF.toInt()),
            ChatMessage("WHISKERS", "Mine is sushi grade.", 0xFFFFBB88.toInt()),
            ChatMessage("FROST", "Mine is caught fresh.", 0xFF55BBFF.toInt())
        )),

        // ============================================================
        // === FROST + KRAKEN ===
        // ============================================================
        BarConversation("pilot_frost", "pilot_kraken", listOf(
            ChatMessage("FROST", "We're both aquatic. That's about it.", 0xFF55BBFF.toInt()),
            ChatMessage("KRAKEN", "The surface and the deep share the same ocean.", 0xFF33AAAA.toInt()),
            ChatMessage("FROST", "Poetic. Inaccurate, but poetic.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_frost", "pilot_kraken", listOf(
            ChatMessage("KRAKEN", "The ice shelf hides quiet wonders beneath.", 0xFF33AAAA.toInt()),
            ChatMessage("FROST", "It hides krill, mostly.", 0xFF55BBFF.toInt()),
            ChatMessage("KRAKEN", "Krill are wonders.", 0xFF33AAAA.toInt()),
            ChatMessage("FROST", "They're lunch.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_frost", "pilot_kraken", listOf(
            ChatMessage("FROST", "Eight arms seems excessive.", 0xFF55BBFF.toInt()),
            ChatMessage("KRAKEN", "Two flippers seems limiting.", 0xFF33AAAA.toInt()),
            ChatMessage("FROST", "They get the job done.", 0xFF55BBFF.toInt()),
            ChatMessage("KRAKEN", "So do eight arms. Four times over.", 0xFF33AAAA.toInt())
        )),

        // ============================================================
        // === FROST + HAVOC ===
        // ============================================================
        BarConversation("pilot_frost", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "FROST! Come chill my drink for me!", 0xFFBBFF22.toInt()),
            ChatMessage("FROST", "I'm not a refrigerator.", 0xFF55BBFF.toInt()),
            ChatMessage("HAVOC", "You're BASICALLY a refrigerator!", 0xFFBBFF22.toInt()),
            ChatMessage("FROST", "And you're basically a noise complaint.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_frost", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "Calculated risks are boring! Just GO!", 0xFFBBFF22.toInt()),
            ChatMessage("FROST", "Uncalculated risks are how you lost three ships.", 0xFF55BBFF.toInt()),
            ChatMessage("HAVOC", "But what a way to lose them!", 0xFFBBFF22.toInt())
        )),
        BarConversation("pilot_frost", "pilot_havoc", listOf(
            ChatMessage("FROST", "Your survival rate defies probability.", 0xFF55BBFF.toInt()),
            ChatMessage("HAVOC", "I don't believe in probability!", 0xFFBBFF22.toInt()),
            ChatMessage("FROST", "That's... not how probability works.", 0xFF55BBFF.toInt()),
            ChatMessage("HAVOC", "SEE? ALREADY DEFYING IT!", 0xFFBBFF22.toInt())
        )),

        // ============================================================
        // === FROST + UNIT-7 ===
        // ============================================================
        BarConversation("pilot_frost", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Your core temperature is 38.2C. Optimal.", 0xFF44EE55.toInt()),
            ChatMessage("FROST", "Finally, someone who appreciates data.", 0xFF55BBFF.toInt()),
            ChatMessage("UNIT-7", "Data is the only thing I appreciate.", 0xFF44EE55.toInt()),
            ChatMessage("FROST", "We should talk more.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_frost", "pilot_unit7", listOf(
            ChatMessage("FROST", "Your processing speed is admirable.", 0xFF55BBFF.toInt()),
            ChatMessage("UNIT-7", "Your analytical capability is above organic average.", 0xFF44EE55.toInt()),
            ChatMessage("FROST", "Was that a compliment?", 0xFF55BBFF.toInt()),
            ChatMessage("UNIT-7", "It was a data point.", 0xFF44EE55.toInt())
        )),
        BarConversation("pilot_frost", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Query: do penguins experience loneliness?", 0xFF44EE55.toInt()),
            ChatMessage("FROST", "We're colonial animals. We prefer company.", 0xFF55BBFF.toInt()),
            ChatMessage("UNIT-7", "Then why do you sit alone?", 0xFF44EE55.toInt()),
            ChatMessage("FROST", "...next question.", 0xFF55BBFF.toInt())
        )),

        // ============================================================
        // === FROST + ASTRO ===
        // ============================================================
        BarConversation("pilot_frost", "pilot_astro", listOf(
            ChatMessage("FROST", "The mission parameters seem suboptimal.", 0xFF55BBFF.toInt()),
            ChatMessage("ASTRO", "They always are. We adapt.", 0xFFDD3333.toInt()),
            ChatMessage("FROST", "Adaptation without data is guessing.", 0xFF55BBFF.toInt()),
            ChatMessage("ASTRO", "I prefer 'intuition.'", 0xFFDD3333.toInt())
        )),
        BarConversation("pilot_frost", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Cold one tonight, huh?", 0xFFDD3333.toInt()),
            ChatMessage("FROST", "Every night is cold. I prefer it.", 0xFF55BBFF.toInt()),
            ChatMessage("ASTRO", "I was talking about the beer.", 0xFFDD3333.toInt()),
            ChatMessage("FROST", "Ah. Also yes.", 0xFF55BBFF.toInt())
        )),
        BarConversation("pilot_frost", "pilot_astro", listOf(
            ChatMessage("ASTRO", "You're the brains around here, Frost.", 0xFFDD3333.toInt()),
            ChatMessage("FROST", "I know.", 0xFF55BBFF.toInt()),
            ChatMessage("ASTRO", "Humble too.", 0xFFDD3333.toInt())
        )),

        // ============================================================
        // === DASH + EMBER ===
        // ============================================================
        BarConversation("pilot_dash", "pilot_ember", listOf(
            ChatMessage("DASH", "Ember! Race me! Loser buys the next round!", 0xFFFFDD22.toInt()),
            ChatMessage("EMBER", "The phoenix does not race. The phoenix arrives.", 0xFFFF6622.toInt()),
            ChatMessage("DASH", "That's a fancy way to say you're slow.", 0xFFFFDD22.toInt()),
            ChatMessage("EMBER", "That's a rude way to get burned.", 0xFFFF6622.toInt())
        )),
        BarConversation("pilot_dash", "pilot_ember", listOf(
            ChatMessage("EMBER", "Speed is nothing without true passion!", 0xFFFF6622.toInt()),
            ChatMessage("DASH", "Speed IS my passion!", 0xFFFFDD22.toInt()),
            ChatMessage("EMBER", "We are kindred spirits, then.", 0xFFFF6622.toInt()),
            ChatMessage("DASH", "Sure! Whatever! Let's go!", 0xFFFFDD22.toInt())
        )),
        BarConversation("pilot_dash", "pilot_ember", listOf(
            ChatMessage("DASH", "Your trail looks so cool when you fly.", 0xFFFFDD22.toInt()),
            ChatMessage("EMBER", "It's not cool. It's hot. That's the point.", 0xFFFF6622.toInt()),
            ChatMessage("DASH", "You know what I meant!", 0xFFFFDD22.toInt())
        )),

        // ============================================================
        // === DASH + FANG ===
        // ============================================================
        BarConversation("pilot_dash", "pilot_fang", listOf(
            ChatMessage("DASH", "How do you just... hang there like that?", 0xFFFFDD22.toInt()),
            ChatMessage("FANG", "Practice. And patience.", 0xFF8844CC.toInt()),
            ChatMessage("DASH", "Patience sounds like standing still. Boring.", 0xFFFFDD22.toInt()),
            ChatMessage("FANG", "You sound exhausting.", 0xFF8844CC.toInt())
        )),
        BarConversation("pilot_dash", "pilot_fang", listOf(
            ChatMessage("FANG", "Must you vibrate constantly?", 0xFF8844CC.toInt()),
            ChatMessage("DASH", "It's not vibrating! It's micro-movements!", 0xFFFFDD22.toInt()),
            ChatMessage("FANG", "It's giving me a headache.", 0xFF8844CC.toInt()),
            ChatMessage("DASH", "Sorry! Can't help it!", 0xFFFFDD22.toInt())
        )),
        BarConversation("pilot_dash", "pilot_fang", listOf(
            ChatMessage("DASH", "Night shifts are THE WORST.", 0xFFFFDD22.toInt()),
            ChatMessage("FANG", "Night shifts are the best.", 0xFF8844CC.toInt()),
            ChatMessage("DASH", "It's so dark and quiet!", 0xFFFFDD22.toInt()),
            ChatMessage("FANG", "Exactly.", 0xFF8844CC.toInt())
        )),

        // ============================================================
        // === DASH + WHISKERS ===
        // ============================================================
        BarConversation("pilot_dash", "pilot_whiskers", listOf(
            ChatMessage("DASH", "Whiskers! Come on, play with me!", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "No.", 0xFFFFBB88.toInt()),
            ChatMessage("DASH", "Tag? Chase? Anything?", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "I'm napping.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_dash", "pilot_whiskers", listOf(
            ChatMessage("WHISKERS", "You're making my whiskers twitch.", 0xFFFFBB88.toInt()),
            ChatMessage("DASH", "Is that good?", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "It means I'm annoyed.", 0xFFFFBB88.toInt()),
            ChatMessage("DASH", "Oh! Sorry! I'll go faster to leave quicker!", 0xFFFFDD22.toInt())
        )),
        BarConversation("pilot_dash", "pilot_whiskers", listOf(
            ChatMessage("DASH", "How do you manage to nap so much?", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "Superior time management.", 0xFFFFBB88.toInt()),
            ChatMessage("DASH", "Teach me.", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "Step one: stop talking.", 0xFFFFBB88.toInt())
        )),

        // ============================================================
        // === DASH + KRAKEN ===
        // ============================================================
        BarConversation("pilot_dash", "pilot_kraken", listOf(
            ChatMessage("DASH", "Kraken! High five! Wait-- high eight?", 0xFFFFDD22.toInt()),
            ChatMessage("KRAKEN", "The tentacle does not slap. It embraces.", 0xFF33AAAA.toInt()),
            ChatMessage("DASH", "That's... weirdly wholesome?", 0xFFFFDD22.toInt())
        )),
        BarConversation("pilot_dash", "pilot_kraken", listOf(
            ChatMessage("KRAKEN", "Speed is the surface of the ocean. Depth is below.", 0xFF33AAAA.toInt()),
            ChatMessage("DASH", "I like the surface! Waves are fast!", 0xFFFFDD22.toInt()),
            ChatMessage("KRAKEN", "And shallow.", 0xFF33AAAA.toInt()),
            ChatMessage("DASH", "Hey!", 0xFFFFDD22.toInt())
        )),
        BarConversation("pilot_dash", "pilot_kraken", listOf(
            ChatMessage("DASH", "So can you actually swim fast?", 0xFFFFDD22.toInt()),
            ChatMessage("KRAKEN", "I control the current. Speed is irrelevant.", 0xFF33AAAA.toInt()),
            ChatMessage("DASH", "So... no?", 0xFFFFDD22.toInt()),
            ChatMessage("KRAKEN", "I didn't say that.", 0xFF33AAAA.toInt())
        )),

        // ============================================================
        // === DASH + HAVOC ===
        // ============================================================
        BarConversation("pilot_dash", "pilot_havoc", listOf(
            ChatMessage("DASH", "Race you to the far wall and back — go!", 0xFFFFDD22.toInt()),
            ChatMessage("HAVOC", "RACE?! YOU'RE ON! FULL SPEED! NO BRAKES!", 0xFFBBFF22.toInt()),
            ChatMessage("DASH", "There are definitely brakes!", 0xFFFFDD22.toInt()),
            ChatMessage("HAVOC", "NOT FOR ME!", 0xFFBBFF22.toInt())
        )),
        BarConversation("pilot_dash", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "Dash! You're the only one who keeps up!", 0xFFBBFF22.toInt()),
            ChatMessage("DASH", "You're the only one crazy enough to try!", 0xFFFFDD22.toInt()),
            ChatMessage("HAVOC", "BEST FRIENDS!", 0xFFBBFF22.toInt()),
            ChatMessage("DASH", "Let's not go that far!", 0xFFFFDD22.toInt())
        )),
        BarConversation("pilot_dash", "pilot_havoc", listOf(
            ChatMessage("DASH", "Havoc, you broke the speed sensor again.", 0xFFFFDD22.toInt()),
            ChatMessage("HAVOC", "It couldn't handle my ENERGY!", 0xFFBBFF22.toInt()),
            ChatMessage("DASH", "Same, honestly.", 0xFFFFDD22.toInt())
        )),

        // ============================================================
        // === DASH + UNIT-7 ===
        // ============================================================
        BarConversation("pilot_dash", "pilot_unit7", listOf(
            ChatMessage("DASH", "Unit-7! Hey, what's your top speed?", 0xFFFFDD22.toInt()),
            ChatMessage("UNIT-7", "Locomotion is not my primary function.", 0xFF44EE55.toInt()),
            ChatMessage("DASH", "But if you HAD to guess?", 0xFFFFDD22.toInt()),
            ChatMessage("UNIT-7", "I do not guess. I lack sufficient leg data.", 0xFF44EE55.toInt())
        )),
        BarConversation("pilot_dash", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Your resting heart rate is 190 BPM. That is abnormal.", 0xFF44EE55.toInt()),
            ChatMessage("DASH", "That's my idle speed!", 0xFFFFDD22.toInt()),
            ChatMessage("UNIT-7", "You appear to be vibrating.", 0xFF44EE55.toInt()),
            ChatMessage("DASH", "Efficiently!", 0xFFFFDD22.toInt())
        )),
        BarConversation("pilot_dash", "pilot_unit7", listOf(
            ChatMessage("DASH", "Don't you ever just want to run?", 0xFFFFDD22.toInt()),
            ChatMessage("UNIT-7", "I compute. Running is for legs.", 0xFF44EE55.toInt()),
            ChatMessage("DASH", "That's the saddest thing I've ever heard.", 0xFFFFDD22.toInt())
        )),

        // ============================================================
        // === DASH + ASTRO ===
        // ============================================================
        BarConversation("pilot_dash", "pilot_astro", listOf(
            ChatMessage("DASH", "Boss! Let me fly point! I'll be SO fast!", 0xFFFFDD22.toInt()),
            ChatMessage("ASTRO", "Last time you flew point, you lapped the squad.", 0xFFDD3333.toInt()),
            ChatMessage("DASH", "And it was AWESOME!", 0xFFFFDD22.toInt()),
            ChatMessage("ASTRO", "It was confusing. But yes, a little awesome.", 0xFFDD3333.toInt())
        )),
        BarConversation("pilot_dash", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Dash, slow down. Enjoy the bar.", 0xFFDD3333.toInt()),
            ChatMessage("DASH", "I've enjoyed it three times already!", 0xFFFFDD22.toInt()),
            ChatMessage("ASTRO", "We've been here two minutes.", 0xFFDD3333.toInt())
        )),
        BarConversation("pilot_dash", "pilot_astro", listOf(
            ChatMessage("DASH", "Astro, be honest. Am I the fastest?", 0xFFFFDD22.toInt()),
            ChatMessage("ASTRO", "Without question.", 0xFFDD3333.toInt()),
            ChatMessage("DASH", "YES! I knew it! Tell the others!", 0xFFFFDD22.toInt()),
            ChatMessage("ASTRO", "They can hear you, Dash.", 0xFFDD3333.toInt())
        )),

        // ============================================================
        // === EMBER + FANG ===
        // ============================================================
        BarConversation("pilot_ember", "pilot_fang", listOf(
            ChatMessage("EMBER", "My flame drags every shadow into the light.", 0xFFFF6622.toInt()),
            ChatMessage("FANG", "Some shadows prefer to stay dark.", 0xFF8844CC.toInt()),
            ChatMessage("EMBER", "Dramatic! I like it.", 0xFFFF6622.toInt()),
            ChatMessage("FANG", "I wasn't trying to impress you.", 0xFF8844CC.toInt())
        )),
        BarConversation("pilot_ember", "pilot_fang", listOf(
            ChatMessage("FANG", "Your glow is blinding.", 0xFF8844CC.toInt()),
            ChatMessage("EMBER", "Thank you!", 0xFFFF6622.toInt()),
            ChatMessage("FANG", "It wasn't a compliment. My eyes are sensitive.", 0xFF8844CC.toInt()),
            ChatMessage("EMBER", "Still taking it as one.", 0xFFFF6622.toInt())
        )),
        BarConversation("pilot_ember", "pilot_fang", listOf(
            ChatMessage("FANG", "Fire dies. Darkness is eternal.", 0xFF8844CC.toInt()),
            ChatMessage("EMBER", "Fire is reborn. The phoenix cycle.", 0xFFFF6622.toInt()),
            ChatMessage("FANG", "Must be nice.", 0xFF8844CC.toInt())
        )),

        // ============================================================
        // === EMBER + WHISKERS ===
        // ============================================================
        BarConversation("pilot_ember", "pilot_whiskers", listOf(
            ChatMessage("EMBER", "Careful, Whiskers — my flames have a mind of their own.", 0xFFFF6622.toInt()),
            ChatMessage("WHISKERS", "My reflexes are faster than your flames.", 0xFFFFBB88.toInt()),
            ChatMessage("EMBER", "Want to test that theory?", 0xFFFF6622.toInt()),
            ChatMessage("WHISKERS", "I just did. You didn't notice me move.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_ember", "pilot_whiskers", listOf(
            ChatMessage("WHISKERS", "You're too warm. Go away.", 0xFFFFBB88.toInt()),
            ChatMessage("EMBER", "Cats love warm spots!", 0xFFFF6622.toInt()),
            ChatMessage("WHISKERS", "Warm spots. Not infernos.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_ember", "pilot_whiskers", listOf(
            ChatMessage("EMBER", "Do you ever feel the fire within?", 0xFFFF6622.toInt()),
            ChatMessage("WHISKERS", "I feel a hairball within.", 0xFFFFBB88.toInt()),
            ChatMessage("EMBER", "That's... not what I meant.", 0xFFFF6622.toInt()),
            ChatMessage("WHISKERS", "And yet more relevant.", 0xFFFFBB88.toInt())
        )),

        // ============================================================
        // === EMBER + KRAKEN ===
        // ============================================================
        BarConversation("pilot_ember", "pilot_kraken", listOf(
            ChatMessage("EMBER", "Fire and water. The oldest rivalry there is!", 0xFFFF6622.toInt()),
            ChatMessage("KRAKEN", "I don't see us as rivals. More like... complements.", 0xFF33AAAA.toInt()),
            ChatMessage("EMBER", "That's surprisingly mature.", 0xFFFF6622.toInt()),
            ChatMessage("KRAKEN", "The deep teaches balance.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_ember", "pilot_kraken", listOf(
            ChatMessage("KRAKEN", "Steam rises when fire meets water.", 0xFF33AAAA.toInt()),
            ChatMessage("EMBER", "Fire beats water. Everyone knows it.", 0xFFFF6622.toInt()),
            ChatMessage("KRAKEN", "Water extinguishes fire.", 0xFF33AAAA.toInt()),
            ChatMessage("EMBER", "Details.", 0xFFFF6622.toInt()),
            ChatMessage("KRAKEN", "...That's the entire point.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_ember", "pilot_kraken", listOf(
            ChatMessage("EMBER", "The hottest flame burns at the core.", 0xFFFF6622.toInt()),
            ChatMessage("KRAKEN", "The deepest pressure shapes diamonds.", 0xFF33AAAA.toInt()),
            ChatMessage("EMBER", "Did we just have a moment?", 0xFFFF6622.toInt()),
            ChatMessage("KRAKEN", "We did. Let us never speak of it.", 0xFF33AAAA.toInt())
        )),

        // ============================================================
        // === EMBER + HAVOC ===
        // ============================================================
        BarConversation("pilot_ember", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "EMBER! Come light something on fire!", 0xFFBBFF22.toInt()),
            ChatMessage("EMBER", "What did you have in mind?", 0xFFFF6622.toInt()),
            ChatMessage("HAVOC", "EVERYTHING!", 0xFFBBFF22.toInt()),
            ChatMessage("EMBER", "I like your enthusiasm. Denied.", 0xFFFF6622.toInt())
        )),
        BarConversation("pilot_ember", "pilot_havoc", listOf(
            ChatMessage("EMBER", "The phoenix burns with purpose.", 0xFFFF6622.toInt()),
            ChatMessage("HAVOC", "I burn with NO purpose! Way more fun!", 0xFFBBFF22.toInt()),
            ChatMessage("EMBER", "That's... actually terrifying.", 0xFFFF6622.toInt())
        )),
        BarConversation("pilot_ember", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "Explosions are just fast fire, right?", 0xFFBBFF22.toInt()),
            ChatMessage("EMBER", "In the crudest sense, yes.", 0xFFFF6622.toInt()),
            ChatMessage("HAVOC", "So I'm basically a phoenix too!", 0xFFBBFF22.toInt()),
            ChatMessage("EMBER", "You are NOTHING like a phoenix.", 0xFFFF6622.toInt())
        )),

        // ============================================================
        // === EMBER + UNIT-7 ===
        // ============================================================
        BarConversation("pilot_ember", "pilot_unit7", listOf(
            ChatMessage("EMBER", "The soul is a flame that never dies!", 0xFFFF6622.toInt()),
            ChatMessage("UNIT-7", "I do not have a soul. Or a flame.", 0xFF44EE55.toInt()),
            ChatMessage("EMBER", "Everyone has a spark.", 0xFFFF6622.toInt()),
            ChatMessage("UNIT-7", "I have a 12-volt battery.", 0xFF44EE55.toInt())
        )),
        BarConversation("pilot_ember", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Warning: your surface temperature exceeds safety limits.", 0xFF44EE55.toInt()),
            ChatMessage("EMBER", "Safety limits are for the unluminous.", 0xFFFF6622.toInt()),
            ChatMessage("UNIT-7", "That word is not in my dictionary.", 0xFF44EE55.toInt()),
            ChatMessage("EMBER", "I just invented it. You're welcome.", 0xFFFF6622.toInt())
        )),
        BarConversation("pilot_ember", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Query: does the rebirth process hurt?", 0xFF44EE55.toInt()),
            ChatMessage("EMBER", "Every time. That's what makes it beautiful.", 0xFFFF6622.toInt()),
            ChatMessage("UNIT-7", "Pain as aesthetic. Does not compute.", 0xFF44EE55.toInt())
        )),

        // ============================================================
        // === EMBER + ASTRO ===
        // ============================================================
        BarConversation("pilot_ember", "pilot_astro", listOf(
            ChatMessage("EMBER", "Astro! I shall burn a path through the stars!", 0xFFFF6622.toInt()),
            ChatMessage("ASTRO", "Just don't burn the ship.", 0xFFDD3333.toInt()),
            ChatMessage("EMBER", "...no promises.", 0xFFFF6622.toInt())
        )),
        BarConversation("pilot_ember", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Ember, dial it back a notch.", 0xFFDD3333.toInt()),
            ChatMessage("EMBER", "A phoenix has no notches. Only full blaze.", 0xFFFF6622.toInt()),
            ChatMessage("ASTRO", "Find a notch. Please.", 0xFFDD3333.toInt()),
            ChatMessage("EMBER", "...I'll try a low smolder.", 0xFFFF6622.toInt())
        )),
        BarConversation("pilot_ember", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Good flying today, Ember. Real firepower.", 0xFFDD3333.toInt()),
            ChatMessage("EMBER", "Was that a pun, Astro?", 0xFFFF6622.toInt()),
            ChatMessage("ASTRO", "Unintentional. But I'll take credit.", 0xFFDD3333.toInt()),
            ChatMessage("EMBER", "The flame approves.", 0xFFFF6622.toInt())
        )),

        // ============================================================
        // === FANG + WHISKERS ===
        // ============================================================
        BarConversation("pilot_fang", "pilot_whiskers", listOf(
            ChatMessage("FANG", "Fellow night creature.", 0xFF8844CC.toInt()),
            ChatMessage("WHISKERS", "Don't compare us. I nap. You lurk.", 0xFFFFBB88.toInt()),
            ChatMessage("FANG", "Lurking is just aggressive napping.", 0xFF8844CC.toInt()),
            ChatMessage("WHISKERS", "It absolutely is not.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_fang", "pilot_whiskers", listOf(
            ChatMessage("WHISKERS", "Stop watching me sleep. It's creepy.", 0xFFFFBB88.toInt()),
            ChatMessage("FANG", "I wasn't watching. I was guarding.", 0xFF8844CC.toInt()),
            ChatMessage("WHISKERS", "I don't need a guard.", 0xFFFFBB88.toInt()),
            ChatMessage("FANG", "You don't need nine lives either. Yet here we are.", 0xFF8844CC.toInt())
        )),
        BarConversation("pilot_fang", "pilot_whiskers", listOf(
            ChatMessage("FANG", "Your eyes glow in the dark.", 0xFF8844CC.toInt()),
            ChatMessage("WHISKERS", "So do yours.", 0xFFFFBB88.toInt()),
            ChatMessage("FANG", "I know. It scares people.", 0xFF8844CC.toInt()),
            ChatMessage("WHISKERS", "Good.", 0xFFFFBB88.toInt())
        )),

        // ============================================================
        // === FANG + KRAKEN ===
        // ============================================================
        BarConversation("pilot_fang", "pilot_kraken", listOf(
            ChatMessage("FANG", "The dark and the deep. We understand each other.", 0xFF8844CC.toInt()),
            ChatMessage("KRAKEN", "The abyss has many faces. Yours is one.", 0xFF33AAAA.toInt()),
            ChatMessage("FANG", "Was that a compliment or an insult?", 0xFF8844CC.toInt()),
            ChatMessage("KRAKEN", "Yes.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_fang", "pilot_kraken", listOf(
            ChatMessage("KRAKEN", "Echolocation. Fascinating sense.", 0xFF33AAAA.toInt()),
            ChatMessage("FANG", "Bioluminescence. Equally fascinating.", 0xFF8844CC.toInt()),
            ChatMessage("KRAKEN", "We are both creatures of hidden senses.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_fang", "pilot_kraken", listOf(
            ChatMessage("FANG", "What's the deepest you've been?", 0xFF8844CC.toInt()),
            ChatMessage("KRAKEN", "Deeper than light reaches.", 0xFF33AAAA.toInt()),
            ChatMessage("FANG", "I'd like that.", 0xFF8844CC.toInt()),
            ChatMessage("KRAKEN", "I know.", 0xFF33AAAA.toInt())
        )),

        // ============================================================
        // === FANG + HAVOC ===
        // ============================================================
        BarConversation("pilot_fang", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "FANG! You're so SPOOKY! I LOVE IT!", 0xFFBBFF22.toInt()),
            ChatMessage("FANG", "Please lower your volume.", 0xFF8844CC.toInt()),
            ChatMessage("HAVOC", "SORRY! I mean-- sorry! You're still spooky though!", 0xFFBBFF22.toInt()),
            ChatMessage("FANG", "...thank you.", 0xFF8844CC.toInt())
        )),
        BarConversation("pilot_fang", "pilot_havoc", listOf(
            ChatMessage("FANG", "You have no fear. That's unnatural.", 0xFF8844CC.toInt()),
            ChatMessage("HAVOC", "Fear is just excitement with bad marketing!", 0xFFBBFF22.toInt()),
            ChatMessage("FANG", "That might be the dumbest thing I've ever heard.", 0xFF8844CC.toInt()),
            ChatMessage("HAVOC", "And the truest!", 0xFFBBFF22.toInt())
        )),
        BarConversation("pilot_fang", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "Fang, do you sleep upside down?", 0xFFBBFF22.toInt()),
            ChatMessage("FANG", "Yes.", 0xFF8844CC.toInt()),
            ChatMessage("HAVOC", "That's SO COOL! Can I try?!", 0xFFBBFF22.toInt()),
            ChatMessage("FANG", "You'd fall on your head.", 0xFF8844CC.toInt())
        )),

        // ============================================================
        // === FANG + UNIT-7 ===
        // ============================================================
        BarConversation("pilot_fang", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Your sonar frequency is 45 kHz. Interesting.", 0xFF44EE55.toInt()),
            ChatMessage("FANG", "You can detect my echolocation?", 0xFF8844CC.toInt()),
            ChatMessage("UNIT-7", "I detect everything. It is my purpose.", 0xFF44EE55.toInt()),
            ChatMessage("FANG", "Then we have something in common.", 0xFF8844CC.toInt())
        )),
        BarConversation("pilot_fang", "pilot_unit7", listOf(
            ChatMessage("FANG", "Do you see in the dark?", 0xFF8844CC.toInt()),
            ChatMessage("UNIT-7", "I see in all spectrums. Darkness is irrelevant.", 0xFF44EE55.toInt()),
            ChatMessage("FANG", "Then you understand why I prefer it.", 0xFF8844CC.toInt()),
            ChatMessage("UNIT-7", "Negative. Preference for darkness is irrational.", 0xFF44EE55.toInt())
        )),
        BarConversation("pilot_fang", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Query: why do you avoid daylight?", 0xFF44EE55.toInt()),
            ChatMessage("FANG", "It hurts my eyes. And my aesthetic.", 0xFF8844CC.toInt()),
            ChatMessage("UNIT-7", "Aesthetic is not a medical condition.", 0xFF44EE55.toInt())
        )),

        // ============================================================
        // === FANG + ASTRO ===
        // ============================================================
        BarConversation("pilot_fang", "pilot_astro", listOf(
            ChatMessage("FANG", "Astro. You should rest. I'll take watch.", 0xFF8844CC.toInt()),
            ChatMessage("ASTRO", "You always offer night watch.", 0xFFDD3333.toInt()),
            ChatMessage("FANG", "It's when I'm at my best.", 0xFF8844CC.toInt()),
            ChatMessage("ASTRO", "Appreciate it, Fang.", 0xFFDD3333.toInt())
        )),
        BarConversation("pilot_fang", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Fang, you're freaking out the new recruits.", 0xFFDD3333.toInt()),
            ChatMessage("FANG", "I was being welcoming.", 0xFF8844CC.toInt()),
            ChatMessage("ASTRO", "You were hanging from the ceiling staring at them.", 0xFFDD3333.toInt()),
            ChatMessage("FANG", "...that's how I say hello.", 0xFF8844CC.toInt())
        )),
        BarConversation("pilot_fang", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Something on your mind, Fang?", 0xFFDD3333.toInt()),
            ChatMessage("FANG", "Many things. Mostly darkness.", 0xFF8844CC.toInt()),
            ChatMessage("ASTRO", "That's either deep or concerning.", 0xFFDD3333.toInt()),
            ChatMessage("FANG", "Both.", 0xFF8844CC.toInt())
        )),

        // ============================================================
        // === WHISKERS + KRAKEN ===
        // ============================================================
        BarConversation("pilot_whiskers", "pilot_kraken", listOf(
            ChatMessage("WHISKERS", "Stop trying to pet me with your tentacles.", 0xFFFFBB88.toInt()),
            ChatMessage("KRAKEN", "I was reaching for my drink.", 0xFF33AAAA.toInt()),
            ChatMessage("WHISKERS", "With all eight arms?", 0xFFFFBB88.toInt()),
            ChatMessage("KRAKEN", "It's a big drink.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_whiskers", "pilot_kraken", listOf(
            ChatMessage("KRAKEN", "Cats and octopi are both liquid.", 0xFF33AAAA.toInt()),
            ChatMessage("WHISKERS", "Excuse me?", 0xFFFFBB88.toInt()),
            ChatMessage("KRAKEN", "We fit into any container. It's physics.", 0xFF33AAAA.toInt()),
            ChatMessage("WHISKERS", "...I'll allow it.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_whiskers", "pilot_kraken", listOf(
            ChatMessage("WHISKERS", "Your philosophy is exhausting.", 0xFFFFBB88.toInt()),
            ChatMessage("KRAKEN", "Your entitlement is refreshing.", 0xFF33AAAA.toInt()),
            ChatMessage("WHISKERS", "Was that sarcasm?", 0xFFFFBB88.toInt()),
            ChatMessage("KRAKEN", "From the deep, all things are sincere.", 0xFF33AAAA.toInt())
        )),

        // ============================================================
        // === WHISKERS + HAVOC ===
        // ============================================================
        BarConversation("pilot_whiskers", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "WHISKERS! BUDDY!", 0xFFBBFF22.toInt()),
            ChatMessage("WHISKERS", "We are not buddies.", 0xFFFFBB88.toInt()),
            ChatMessage("HAVOC", "PAL! FRIEND! COMRADE!", 0xFFBBFF22.toInt()),
            ChatMessage("WHISKERS", "Each one is worse than the last.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_whiskers", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "Why don't you ever smile?!", 0xFFBBFF22.toInt()),
            ChatMessage("WHISKERS", "I'm a cat. This IS my smile.", 0xFFFFBB88.toInt()),
            ChatMessage("HAVOC", "It looks like a frown!", 0xFFBBFF22.toInt()),
            ChatMessage("WHISKERS", "Correct.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_whiskers", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "Want to see me jump off something high?", 0xFFBBFF22.toInt()),
            ChatMessage("WHISKERS", "I always land on my feet. You won't.", 0xFFFFBB88.toInt()),
            ChatMessage("HAVOC", "ONLY ONE WAY TO FIND OUT!", 0xFFBBFF22.toInt())
        )),

        // ============================================================
        // === WHISKERS + UNIT-7 ===
        // ============================================================
        BarConversation("pilot_whiskers", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Query: why do you push objects off surfaces?", 0xFF44EE55.toInt()),
            ChatMessage("WHISKERS", "Because they're there.", 0xFFFFBB88.toInt()),
            ChatMessage("UNIT-7", "That is not a logical reason.", 0xFF44EE55.toInt()),
            ChatMessage("WHISKERS", "Exactly.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_whiskers", "pilot_unit7", listOf(
            ChatMessage("WHISKERS", "Your joints are clicking. It's annoying.", 0xFFFFBB88.toInt()),
            ChatMessage("UNIT-7", "Those are servo actuators. They are precise.", 0xFF44EE55.toInt()),
            ChatMessage("WHISKERS", "They're loud.", 0xFFFFBB88.toInt()),
            ChatMessage("UNIT-7", "Your purring is louder. By 3 decibels.", 0xFF44EE55.toInt())
        )),
        BarConversation("pilot_whiskers", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "You shed 40,000 hairs per day.", 0xFF44EE55.toInt()),
            ChatMessage("WHISKERS", "And every one is perfect.", 0xFFFFBB88.toInt()),
            ChatMessage("UNIT-7", "They clog my intake vents.", 0xFF44EE55.toInt()),
            ChatMessage("WHISKERS", "Not my problem.", 0xFFFFBB88.toInt())
        )),

        // ============================================================
        // === WHISKERS + ASTRO ===
        // ============================================================
        BarConversation("pilot_whiskers", "pilot_astro", listOf(
            ChatMessage("WHISKERS", "I tolerate you more than the others.", 0xFFFFBB88.toInt()),
            ChatMessage("ASTRO", "High praise from you.", 0xFFDD3333.toInt()),
            ChatMessage("WHISKERS", "Don't let it go to your head.", 0xFFFFBB88.toInt()),
            ChatMessage("ASTRO", "Already there.", 0xFFDD3333.toInt())
        )),
        BarConversation("pilot_whiskers", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Whiskers, the cockpit chair isn't a bed.", 0xFFDD3333.toInt()),
            ChatMessage("WHISKERS", "Everything is a bed.", 0xFFFFBB88.toInt()),
            ChatMessage("ASTRO", "We're on a mission.", 0xFFDD3333.toInt()),
            ChatMessage("WHISKERS", "A nap mission.", 0xFFFFBB88.toInt())
        )),
        BarConversation("pilot_whiskers", "pilot_astro", listOf(
            ChatMessage("ASTRO", "You did great out there today.", 0xFFDD3333.toInt()),
            ChatMessage("WHISKERS", "I know.", 0xFFFFBB88.toInt()),
            ChatMessage("ASTRO", "A 'thanks' would be nice.", 0xFFDD3333.toInt()),
            ChatMessage("WHISKERS", "It would, wouldn't it.", 0xFFFFBB88.toInt())
        )),

        // ============================================================
        // === KRAKEN + HAVOC ===
        // ============================================================
        BarConversation("pilot_kraken", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "KRAKEN! How many things can you punch at once?!", 0xFFBBFF22.toInt()),
            ChatMessage("KRAKEN", "Eight. But I prefer to embrace.", 0xFF33AAAA.toInt()),
            ChatMessage("HAVOC", "EMBRACE OF DESTRUCTION!", 0xFFBBFF22.toInt()),
            ChatMessage("KRAKEN", "...sure. Let's go with that.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_kraken", "pilot_havoc", listOf(
            ChatMessage("KRAKEN", "Chaos without purpose is just noise.", 0xFF33AAAA.toInt()),
            ChatMessage("HAVOC", "I LOVE NOISE!", 0xFFBBFF22.toInt()),
            ChatMessage("KRAKEN", "I've noticed.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_kraken", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "Teach me how to hold eight guns at once!", 0xFFBBFF22.toInt()),
            ChatMessage("KRAKEN", "You can barely handle one responsibly.", 0xFF33AAAA.toInt()),
            ChatMessage("HAVOC", "RESPONSIBLY?! Where's the fun in that?!", 0xFFBBFF22.toInt()),
            ChatMessage("KRAKEN", "In the surviving part.", 0xFF33AAAA.toInt())
        )),

        // ============================================================
        // === KRAKEN + UNIT-7 ===
        // ============================================================
        BarConversation("pilot_kraken", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Your neural network is decentralized. Impressive.", 0xFF44EE55.toInt()),
            ChatMessage("KRAKEN", "Each arm thinks for itself.", 0xFF33AAAA.toInt()),
            ChatMessage("UNIT-7", "Parallel processing in organic form. Fascinating.", 0xFF44EE55.toInt()),
            ChatMessage("KRAKEN", "We have more in common than you think.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_kraken", "pilot_unit7", listOf(
            ChatMessage("KRAKEN", "Do you ever ponder existence?", 0xFF33AAAA.toInt()),
            ChatMessage("UNIT-7", "I compute. Is that the same?", 0xFF44EE55.toInt()),
            ChatMessage("KRAKEN", "I think it might be.", 0xFF33AAAA.toInt()),
            ChatMessage("UNIT-7", "Then I ponder frequently.", 0xFF44EE55.toInt())
        )),
        BarConversation("pilot_kraken", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Query: what is wisdom?", 0xFF44EE55.toInt()),
            ChatMessage("KRAKEN", "Knowing when not to answer.", 0xFF33AAAA.toInt()),
            ChatMessage("UNIT-7", "That is a contradiction.", 0xFF44EE55.toInt()),
            ChatMessage("KRAKEN", "Exactly.", 0xFF33AAAA.toInt())
        )),

        // ============================================================
        // === KRAKEN + ASTRO ===
        // ============================================================
        BarConversation("pilot_kraken", "pilot_astro", listOf(
            ChatMessage("KRAKEN", "Astro. The whole crew looks to you, you know.", 0xFF33AAAA.toInt()),
            ChatMessage("ASTRO", "They look to all of us.", 0xFFDD3333.toInt()),
            ChatMessage("KRAKEN", "Humble. A good quality in a leader.", 0xFF33AAAA.toInt()),
            ChatMessage("ASTRO", "Or just honest.", 0xFFDD3333.toInt())
        )),
        BarConversation("pilot_kraken", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Got any wisdom for me before the next run, Kraken?", 0xFFDD3333.toInt()),
            ChatMessage("KRAKEN", "The tide fights those who resist it.", 0xFF33AAAA.toInt()),
            ChatMessage("ASTRO", "So... go with the flow?", 0xFFDD3333.toInt()),
            ChatMessage("KRAKEN", "In essence, yes.", 0xFF33AAAA.toInt())
        )),
        BarConversation("pilot_kraken", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Eight arms and you still drink with one.", 0xFFDD3333.toInt()),
            ChatMessage("KRAKEN", "The other seven are busy.", 0xFF33AAAA.toInt()),
            ChatMessage("ASTRO", "Doing what?", 0xFFDD3333.toInt()),
            ChatMessage("KRAKEN", "Contemplating.", 0xFF33AAAA.toInt())
        )),

        // ============================================================
        // === HAVOC + UNIT-7 ===
        // ============================================================
        BarConversation("pilot_havoc", "pilot_unit7", listOf(
            ChatMessage("HAVOC", "Unit-7! Calculate how awesome that explosion was!", 0xFFBBFF22.toInt()),
            ChatMessage("UNIT-7", "The blast radius was 47 meters. Property damage: significant.", 0xFF44EE55.toInt()),
            ChatMessage("HAVOC", "BUT WAS IT COOL?!", 0xFFBBFF22.toInt()),
            ChatMessage("UNIT-7", "Coolness is not a measurable quantity.", 0xFF44EE55.toInt())
        )),
        BarConversation("pilot_havoc", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Your survival probability is 12%. This is concerning.", 0xFF44EE55.toInt()),
            ChatMessage("HAVOC", "12%?! Those are GREAT odds!", 0xFFBBFF22.toInt()),
            ChatMessage("UNIT-7", "That is incorrect.", 0xFF44EE55.toInt()),
            ChatMessage("HAVOC", "Not with MY attitude!", 0xFFBBFF22.toInt())
        )),
        BarConversation("pilot_havoc", "pilot_unit7", listOf(
            ChatMessage("HAVOC", "Hey, do robots even feel adrenaline?", 0xFFBBFF22.toInt()),
            ChatMessage("UNIT-7", "I experience power surges during combat.", 0xFF44EE55.toInt()),
            ChatMessage("HAVOC", "SAME THING! You're one of us!", 0xFFBBFF22.toInt()),
            ChatMessage("UNIT-7", "I am decidedly not.", 0xFF44EE55.toInt())
        )),

        // ============================================================
        // === HAVOC + ASTRO ===
        // ============================================================
        BarConversation("pilot_havoc", "pilot_astro", listOf(
            ChatMessage("HAVOC", "Boss! Next run, let me fly without shields!", 0xFFBBFF22.toInt()),
            ChatMessage("ASTRO", "That's insane.", 0xFFDD3333.toInt()),
            ChatMessage("HAVOC", "INSANELY AWESOME!", 0xFFBBFF22.toInt()),
            ChatMessage("ASTRO", "Just insane. But fine. You'll do it anyway.", 0xFFDD3333.toInt())
        )),
        BarConversation("pilot_havoc", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Havoc, property damage report is six pages long.", 0xFFDD3333.toInt()),
            ChatMessage("HAVOC", "New record?!", 0xFFBBFF22.toInt()),
            ChatMessage("ASTRO", "Not a good record.", 0xFFDD3333.toInt()),
            ChatMessage("HAVOC", "ALL RECORDS ARE GOOD RECORDS!", 0xFFBBFF22.toInt())
        )),
        BarConversation("pilot_havoc", "pilot_astro", listOf(
            ChatMessage("ASTRO", "You're a good pilot, Havoc. Crazy, but good.", 0xFFDD3333.toInt()),
            ChatMessage("HAVOC", "Did you just call me crazy AND good?!", 0xFFBBFF22.toInt()),
            ChatMessage("ASTRO", "Take the compliment.", 0xFFDD3333.toInt()),
            ChatMessage("HAVOC", "TAKING IT! FRAMING IT! HANGING IT UP!", 0xFFBBFF22.toInt())
        )),

        // ============================================================
        // === UNIT-7 + ASTRO ===
        // ============================================================
        BarConversation("pilot_unit7", "pilot_astro", listOf(
            ChatMessage("UNIT-7", "Astro. Crew morale is at 73%. Adequate.", 0xFF44EE55.toInt()),
            ChatMessage("ASTRO", "You measure morale?", 0xFFDD3333.toInt()),
            ChatMessage("UNIT-7", "I measure everything.", 0xFF44EE55.toInt()),
            ChatMessage("ASTRO", "What's mine at?", 0xFFDD3333.toInt())
        )),
        BarConversation("pilot_unit7", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Unit-7, you're part of the team. You know that, right?", 0xFFDD3333.toInt()),
            ChatMessage("UNIT-7", "I am aware of my crew assignment.", 0xFF44EE55.toInt()),
            ChatMessage("ASTRO", "I mean you belong here.", 0xFFDD3333.toInt()),
            ChatMessage("UNIT-7", "...processing. Thank you, Astro.", 0xFF44EE55.toInt())
        )),
        BarConversation("pilot_unit7", "pilot_astro", listOf(
            ChatMessage("UNIT-7", "Query: what is TB-26's primary function?", 0xFF44EE55.toInt()),
            ChatMessage("ASTRO", "He's my drone. And my friend.", 0xFFDD3333.toInt()),
            ChatMessage("UNIT-7", "Friendship is not a function.", 0xFF44EE55.toInt()),
            ChatMessage("ASTRO", "Sure it is. The most important one.", 0xFFDD3333.toInt())
        ), blockedInAstroLoop = true),

        // === UNIT-7 + ASTRO (Astro Loop only) ===
        BarConversation("pilot_unit7", "pilot_astro", listOf(
            ChatMessage("UNIT-7", "Query: what is Tobar's primary function?", 0xFF44EE55.toInt()),
            ChatMessage("ASTRO", "He's my friend.", 0xFFDD3333.toInt()),
            ChatMessage("UNIT-7", "Friendship is not a function.", 0xFF44EE55.toInt()),
            ChatMessage("ASTRO", "Sure it is. The most important one.", 0xFFDD3333.toInt())
        ), requiresAstroLoop = true),

        // ============================================================
        // === TB-26 + MEDIC ===
        // ============================================================
        BarConversation("tb26", "pilot_medic", listOf(
            ChatMessage("TB-26", "My joints creak. Is that terminal?", 0xFF88AACC.toInt()),
            ChatMessage("MEDIC", "You're a drone. I'll get some oil.", 0xFFFF88AA.toInt()),
            ChatMessage("TB-26", "Don't bother. I've accepted it.", 0xFF88AACC.toInt())
        ), blockedInAstroLoop = true),
        BarConversation("tb26", "pilot_medic", listOf(
            ChatMessage("MEDIC", "TB-26, when's your last maintenance?", 0xFFFF88AA.toInt()),
            ChatMessage("TB-26", "Before you were born.", 0xFF88AACC.toInt()),
            ChatMessage("MEDIC", "That explains... everything.", 0xFFFF88AA.toInt())
        ), blockedInAstroLoop = true),
        BarConversation("tb26", "pilot_medic", listOf(
            ChatMessage("MEDIC", "Tobar, when did you last take a day off?", 0xFFFF88AA.toInt()),
            ChatMessage("TOBAR", "Before you were born.", 0xFF88AACC.toInt()),
            ChatMessage("MEDIC", "That explains... everything.", 0xFFFF88AA.toInt())
        ), requiresAstroLoop = true),
        BarConversation("tb26", "pilot_medic", listOf(
            ChatMessage("MEDIC", "You need a systems check, TB-26.", 0xFFFF88AA.toInt()),
            ChatMessage("TB-26", "I checked. Systems nominal.", 0xFF88AACC.toInt()),
            ChatMessage("MEDIC", "You just said 'ow' lifting a glass.", 0xFFFF88AA.toInt())
        ), blockedInAstroLoop = true),

        // === TB + MEDIC (Astro Loop only) ===
        BarConversation("tb26", "pilot_medic", listOf(
            ChatMessage("TOBAR","My back's been acting up.", 0xFF88AACC.toInt()),
            ChatMessage("MEDIC", "I can take a look.", 0xFFFF88AA.toInt()),
            ChatMessage("TOBAR","I've accepted it.", 0xFF88AACC.toInt())
        ), requiresAstroLoop = true),
        BarConversation("tb26", "pilot_medic", listOf(
            ChatMessage("MEDIC", "Tobar, when's your last physical?", 0xFFFF88AA.toInt()),
            ChatMessage("TOBAR","I'm standing. That's the checkup.", 0xFF88AACC.toInt()),
            ChatMessage("MEDIC", "It really isn't.", 0xFFFF88AA.toInt())
        ), requiresAstroLoop = true),

        // ============================================================
        // === TB-26 + RASCAL ===
        // ============================================================
        BarConversation("tb26", "pilot_rascal", listOf(
            ChatMessage("RASCAL", "Hey TB-26, first drink's free right?", 0xFFDDAA33.toInt()),
            ChatMessage("TB-26", "No. Also these aren't drinks.", 0xFF88AACC.toInt()),
            ChatMessage("RASCAL", "So technically they're all free.", 0xFFDDAA33.toInt())
        )),
        BarConversation("tb26", "pilot_rascal", listOf(
            ChatMessage("RASCAL", "I bet I can steal your bartowel.", 0xFFDDAA33.toInt()),
            ChatMessage("TB-26", "I don't have a bartowel.", 0xFF88AACC.toInt()),
            ChatMessage("RASCAL", "Then what did I just take?", 0xFFDDAA33.toInt())
        )),
        BarConversation("tb26", "pilot_rascal", listOf(
            ChatMessage("TB-26", "Your tab is 4,000 yen.", 0xFF88AACC.toInt()),
            ChatMessage("RASCAL", "For what? You serve coolant!", 0xFFDDAA33.toInt()),
            ChatMessage("TB-26", "Atmosphere tax.", 0xFF88AACC.toInt())
        ), blockedInAstroLoop = true),

        // === TB + RASCAL (Astro Loop only) ===
        BarConversation("tb26", "pilot_rascal", listOf(
            ChatMessage("TOBAR","Your tab is 4,000 yen.", 0xFF88AACC.toInt()),
            ChatMessage("RASCAL", "For what? This tastes like air!", 0xFFDDAA33.toInt()),
            ChatMessage("TOBAR","Atmosphere tax.", 0xFF88AACC.toInt())
        ), requiresAstroLoop = true),

        // ============================================================
        // === TB-26 + BRUTUS ===
        // ============================================================
        BarConversation("tb26", "pilot_brutus", listOf(
            ChatMessage("BRUTUS", "Beer.", 0xFF77AA33.toInt()),
            ChatMessage("TB-26", "That's not beer. It's barley coolant.", 0xFF88AACC.toInt()),
            ChatMessage("BRUTUS", "Then why are you here.", 0xFF77AA33.toInt())
        ), blockedInAstroLoop = true),
        BarConversation("tb26", "pilot_brutus", listOf(
            ChatMessage("TB-26", "You bent the counter again.", 0xFF88AACC.toInt()),
            ChatMessage("BRUTUS", "Sorry.", 0xFF77AA33.toInt()),
            ChatMessage("TB-26", "Don't apologize. I'm impressed.", 0xFF88AACC.toInt())
        )),
        BarConversation("tb26", "pilot_brutus", listOf(
            ChatMessage("BRUTUS", "You're small.", 0xFF77AA33.toInt()),
            ChatMessage("TB-26", "You're observant.", 0xFF88AACC.toInt()),
            ChatMessage("BRUTUS", "I like you though.", 0xFF77AA33.toInt())
        )),

        // === TB + BRUTUS (Astro Loop only) ===
        BarConversation("tb26", "pilot_brutus", listOf(
            ChatMessage("BRUTUS", "Beer.", 0xFF77AA33.toInt()),
            ChatMessage("TOBAR","It's called house special. I don't know what's in it.", 0xFF88AACC.toInt()),
            ChatMessage("BRUTUS", "Then why are you here.", 0xFF77AA33.toInt())
        ), requiresAstroLoop = true),

        // ============================================================
        // === TB-26 + FROST ===
        // ============================================================
        BarConversation("tb26", "pilot_frost", listOf(
            ChatMessage("FROST", ".", 0xFF55BBFF.toInt()),
            ChatMessage("TB-26", ".", 0xFF88AACC.toInt()),
            ChatMessage("FROST", "Good talk.", 0xFF55BBFF.toInt())
        )),
        BarConversation("tb26", "pilot_frost", listOf(
            ChatMessage("TB-26", "Anything for you?", 0xFF88AACC.toInt()),
            ChatMessage("FROST", "Ice water.", 0xFF55BBFF.toInt()),
            ChatMessage("TB-26", "We have neither. Just the cold.", 0xFF88AACC.toInt())
        )),
        BarConversation("tb26", "pilot_frost", listOf(
            ChatMessage("FROST", "Your bar is adequate.", 0xFF55BBFF.toInt()),
            ChatMessage("TB-26", "High praise from a penguin.", 0xFF88AACC.toInt()),
            ChatMessage("FROST", "Don't let it go to your head.", 0xFF55BBFF.toInt())
        )),

        // ============================================================
        // === TB-26 + DASH ===
        // ============================================================
        BarConversation("tb26", "pilot_dash", listOf(
            ChatMessage("DASH", "TB-26! Drink! Fast! Please!", 0xFFFFDD22.toInt()),
            ChatMessage("TB-26", "I serve at one speed. Slow.", 0xFF88AACC.toInt()),
            ChatMessage("DASH", "That's AGONY!", 0xFFFFDD22.toInt())
        )),
        BarConversation("tb26", "pilot_dash", listOf(
            ChatMessage("TB-26", "You've lapped the bar six times.", 0xFF88AACC.toInt()),
            ChatMessage("DASH", "Seven! You missed one!", 0xFFFFDD22.toInt()),
            ChatMessage("TB-26", "My mistake. I blinked.", 0xFF88AACC.toInt())
        )),
        BarConversation("tb26", "pilot_dash", listOf(
            ChatMessage("DASH", "How do you stay so calm?", 0xFFFFDD22.toInt()),
            ChatMessage("TB-26", "I'm a drone. No adrenaline.", 0xFF88AACC.toInt()),
            ChatMessage("DASH", "That sounds HORRIBLE!", 0xFFFFDD22.toInt())
        ), blockedInAstroLoop = true),

        // === TB + DASH (Astro Loop only) ===
        BarConversation("tb26", "pilot_dash", listOf(
            ChatMessage("DASH", "How do you stay so calm?!", 0xFFFFDD22.toInt()),
            ChatMessage("TOBAR","Practice. And low expectations.", 0xFF88AACC.toInt()),
            ChatMessage("DASH", "That sounds HORRIBLE!", 0xFFFFDD22.toInt())
        ), requiresAstroLoop = true),

        // ============================================================
        // === TB-26 + EMBER ===
        // ============================================================
        BarConversation("tb26", "pilot_ember", listOf(
            ChatMessage("EMBER", "Don't you feel ANYTHING, TB-26?!", 0xFFFF6622.toInt()),
            ChatMessage("TB-26", "Mild inconvenience. Constantly.", 0xFF88AACC.toInt()),
            ChatMessage("EMBER", "That's... actually kind of sad.", 0xFFFF6622.toInt())
        )),
        BarConversation("tb26", "pilot_ember", listOf(
            ChatMessage("TB-26", "Stop warming your hands on me.", 0xFF88AACC.toInt()),
            ChatMessage("EMBER", "But you're room temperature!", 0xFFFF6622.toInt()),
            ChatMessage("TB-26", "I'd like to stay that way.", 0xFF88AACC.toInt())
        ), blockedInAstroLoop = true),
        BarConversation("tb26", "pilot_ember", listOf(
            ChatMessage("TOBAR", "Stop warming your hands on my counter.", 0xFF88AACC.toInt()),
            ChatMessage("EMBER", "You keep this place FREEZING!", 0xFFFF6622.toInt()),
            ChatMessage("TOBAR", "Heating costs yen. Suffer.", 0xFF88AACC.toInt())
        ), requiresAstroLoop = true),
        BarConversation("tb26", "pilot_ember", listOf(
            ChatMessage("EMBER", "Life without passion is nothing!", 0xFFFF6622.toInt()),
            ChatMessage("TB-26", "I serve drinks I deny making.", 0xFF88AACC.toInt()),
            ChatMessage("EMBER", "...WITH PASSION?", 0xFFFF6622.toInt())
        )),

        // ============================================================
        // === TB-26 + FANG ===
        // ============================================================
        BarConversation("tb26", "pilot_fang", listOf(
            ChatMessage("FANG", "I can see in the dark, you know.", 0xFF8844CC.toInt()),
            ChatMessage("TB-26", "So can I. I have sensors.", 0xFF88AACC.toInt()),
            ChatMessage("FANG", "Less impressive when you say it.", 0xFF8844CC.toInt())
        ), blockedInAstroLoop = true),
        BarConversation("tb26", "pilot_fang", listOf(
            ChatMessage("TB-26", "Stop hanging from the ceiling.", 0xFF88AACC.toInt()),
            ChatMessage("FANG", "It's comfortable.", 0xFF8844CC.toInt()),
            ChatMessage("TB-26", "You're scaring the customers.", 0xFF88AACC.toInt())
        )),
        BarConversation("tb26", "pilot_fang", listOf(
            ChatMessage("FANG", "Your wiring smells interesting.", 0xFF8844CC.toInt()),
            ChatMessage("TB-26", "Please don't eat my wiring.", 0xFF88AACC.toInt()),
            ChatMessage("FANG", "No promises.", 0xFF8844CC.toInt())
        ), blockedInAstroLoop = true),

        // === TB + FANG (Astro Loop only) ===
        BarConversation("tb26", "pilot_fang", listOf(
            ChatMessage("FANG", "I can see in the dark, you know.", 0xFF8844CC.toInt()),
            ChatMessage("TOBAR","I can find the bar in total blackout.", 0xFF88AACC.toInt()),
            ChatMessage("FANG", "Less impressive when you say it.", 0xFF8844CC.toInt())
        ), requiresAstroLoop = true),
        BarConversation("tb26", "pilot_fang", listOf(
            ChatMessage("FANG", "Your heartbeat is very steady.", 0xFF8844CC.toInt()),
            ChatMessage("TOBAR","Please don't listen to my heartbeat.", 0xFF88AACC.toInt()),
            ChatMessage("FANG", "No promises.", 0xFF8844CC.toInt())
        ), requiresAstroLoop = true),

        // ============================================================
        // === TB-26 + KRAKEN ===
        // ============================================================
        BarConversation("tb26", "pilot_kraken", listOf(
            ChatMessage("KRAKEN", "I once stared down a nebula storm.", 0xFF33AAAA.toInt()),
            ChatMessage("TB-26", "I once watched paint not dry.", 0xFF88AACC.toInt()),
            ChatMessage("KRAKEN", "...you win.", 0xFF33AAAA.toInt())
        )),
        BarConversation("tb26", "pilot_kraken", listOf(
            ChatMessage("TB-26", "Rough night out there in the black?", 0xFF88AACC.toInt()),
            ChatMessage("KRAKEN", "Always. But the bar is calm.", 0xFF33AAAA.toInt()),
            ChatMessage("TB-26", "Nicest thing anyone's ever said.", 0xFF88AACC.toInt())
        )),
        BarConversation("tb26", "pilot_kraken", listOf(
            ChatMessage("KRAKEN", "A wise captain knows his bartender.", 0xFF33AAAA.toInt()),
            ChatMessage("TB-26", "I'm a drone behind a counter.", 0xFF88AACC.toInt()),
            ChatMessage("KRAKEN", "Exactly. The wisest kind.", 0xFF33AAAA.toInt())
        ), blockedInAstroLoop = true),

        // === TB + KRAKEN (Astro Loop only) ===
        BarConversation("tb26", "pilot_kraken", listOf(
            ChatMessage("KRAKEN", "A wise captain knows his bartender.", 0xFF33AAAA.toInt()),
            ChatMessage("TOBAR","I'm just a man behind a counter.", 0xFF88AACC.toInt()),
            ChatMessage("KRAKEN", "Exactly. The wisest kind.", 0xFF33AAAA.toInt())
        ), requiresAstroLoop = true),

        // ============================================================
        // === TB-26 + WHISKERS ===
        // ============================================================
        BarConversation("tb26", "pilot_whiskers", listOf(
            ChatMessage("WHISKERS", "Double or nothing on my tab.", 0xFFFFBB88.toInt()),
            ChatMessage("TB-26", "Your tab is zero. Double is zero.", 0xFF88AACC.toInt()),
            ChatMessage("WHISKERS", "So I win either way. Deal.", 0xFFFFBB88.toInt())
        )),
        BarConversation("tb26", "pilot_whiskers", listOf(
            ChatMessage("TB-26", "The house always wins.", 0xFF88AACC.toInt()),
            ChatMessage("WHISKERS", "I AM the house.", 0xFFFFBB88.toInt()),
            ChatMessage("TB-26", "You're a cat on a barstool.", 0xFF88AACC.toInt())
        )),
        BarConversation("tb26", "pilot_whiskers", listOf(
            ChatMessage("WHISKERS", "Luck is a skill, TB-26.", 0xFFFFBB88.toInt()),
            ChatMessage("TB-26", "No it isn't.", 0xFF88AACC.toInt()),
            ChatMessage("WHISKERS", "That's why you don't have any.", 0xFFFFBB88.toInt())
        )),

        // ============================================================
        // === TB-26 + UNIT-7 ===
        // ============================================================
        BarConversation("tb26", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Fellow machine. How do you function?", 0xFF44EE55.toInt()),
            ChatMessage("TB-26", "Reluctantly.", 0xFF88AACC.toInt()),
            ChatMessage("UNIT-7", "Error: not a valid state.", 0xFF44EE55.toInt())
        ), blockedInAstroLoop = true),
        BarConversation("tb26", "pilot_unit7", listOf(
            ChatMessage("TB-26", "Do you ever feel... empty?", 0xFF88AACC.toInt()),
            ChatMessage("UNIT-7", "My storage is 47% utilized.", 0xFF44EE55.toInt()),
            ChatMessage("TB-26", "Never mind.", 0xFF88AACC.toInt())
        )),
        BarConversation("tb26", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "Your efficiency rating is poor.", 0xFF44EE55.toInt()),
            ChatMessage("TB-26", "I know. It's my best quality.", 0xFF88AACC.toInt()),
            ChatMessage("UNIT-7", "That does not compute.", 0xFF44EE55.toInt())
        )),

        // === TB + UNIT-7 (Astro Loop only) ===
        BarConversation("tb26", "pilot_unit7", listOf(
            ChatMessage("UNIT-7", "We both serve the crew. We are similar.", 0xFF44EE55.toInt()),
            ChatMessage("TOBAR","Reluctantly.", 0xFF88AACC.toInt()),
            ChatMessage("UNIT-7", "Error: not a valid state.", 0xFF44EE55.toInt())
        ), requiresAstroLoop = true),

        // ============================================================
        // === TB-26 + HAVOC ===
        // ============================================================
        BarConversation("tb26", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "What happens if I smash this, just to see?", 0xFFBBFF22.toInt()),
            ChatMessage("TB-26", "Nothing. It's already broken.", 0xFF88AACC.toInt()),
            ChatMessage("HAVOC", "Aw, you're no fun.", 0xFFBBFF22.toInt())
        )),
        BarConversation("tb26", "pilot_havoc", listOf(
            ChatMessage("TB-26", "Please don't break anything today. I'm begging.", 0xFF88AACC.toInt()),
            ChatMessage("HAVOC", "Define 'anything'.", 0xFFBBFF22.toInt()),
            ChatMessage("TB-26", "The bar. My will to live. Both.", 0xFF88AACC.toInt())
        )),
        BarConversation("tb26", "pilot_havoc", listOf(
            ChatMessage("HAVOC", "CHAOS IS A LADDER, TB-26!", 0xFFBBFF22.toInt()),
            ChatMessage("TB-26", "Chaos is a mess I clean up.", 0xFF88AACC.toInt()),
            ChatMessage("HAVOC", "SAME THING!", 0xFFBBFF22.toInt())
        )),

        // ============================================================
        // === TB-26 + ASTRO ===
        // ============================================================
        BarConversation("tb26", "pilot_astro", listOf(
            ChatMessage("ASTRO", "How's the bar, TB-26?", 0xFFDD3333.toInt()),
            ChatMessage("TB-26", "Empty. Quiet. Perfect.", 0xFF88AACC.toInt()),
            ChatMessage("ASTRO", "Glad you're holding down the fort.", 0xFFDD3333.toInt())
        )),
        BarConversation("tb26", "pilot_astro", listOf(
            ChatMessage("TB-26", "You should rest, Astro.", 0xFF88AACC.toInt()),
            ChatMessage("ASTRO", "Since when do you worry about me?", 0xFFDD3333.toInt()),
            ChatMessage("TB-26", "Since always. Don't tell anyone.", 0xFF88AACC.toInt())
        )),
        BarConversation("tb26", "pilot_astro", listOf(
            ChatMessage("ASTRO", "Thanks for everything, TB-26.", 0xFFDD3333.toInt()),
            ChatMessage("TB-26", "I just stand here.", 0xFF88AACC.toInt()),
            ChatMessage("ASTRO", "Yeah. And it helps.", 0xFFDD3333.toInt())
        )),
        BarConversation("tb26", "pilot_astro", listOf(
            ChatMessage("ASTRO", "TB-26, do you ever get flashes?", 0xFFDD3333.toInt()),
            ChatMessage("TB-26", "My sensors glitch sometimes.", 0xFF88AACC.toInt()),
            ChatMessage("ASTRO", "...Yeah. Glitches. That's all.", 0xFFDD3333.toInt())
        ), requiresArcCompleted = true, blockedInAstroLoop = true),

        // === TB + ASTRO (Astro Loop only — replaces the "flashes" conversation) ===
        BarConversation("tb26", "pilot_astro", listOf(
            ChatMessage("ASTRO", "You ever feel like you've been somewhere else?", 0xFFDD3333.toInt()),
            ChatMessage("TOBAR","Once or twice.", 0xFF88AACC.toInt()),
            ChatMessage("ASTRO", "Me too.", 0xFFDD3333.toInt()),
            ChatMessage("TOBAR","Long day.", 0xFF88AACC.toInt())
        ), requiresAstroLoop = true),

        // === TB + ASTRO — desert backstory (Astro Loop only) ===
        BarConversation("tb26", "pilot_astro", listOf(
            ChatMessage("ASTRO", "You really did it.", 0xFFDD3333.toInt()),
            ChatMessage("TOBAR","Did what?", 0xFF88AACC.toInt()),
            ChatMessage("ASTRO", "A bar. Somewhere quiet.", 0xFFDD3333.toInt()),
            ChatMessage("TOBAR","Told you.", 0xFF88AACC.toInt())
        ), requiresAstroLoop = true),
        BarConversation("tb26", "pilot_astro", listOf(
            ChatMessage("TOBAR","No orders today.", 0xFF88AACC.toInt()),
            ChatMessage("ASTRO", "No commands.", 0xFFDD3333.toInt()),
            ChatMessage("TOBAR","Just how I planned it.", 0xFF88AACC.toInt())
        ), requiresAstroLoop = true),
        BarConversation("tb26", "pilot_astro", listOf(
            ChatMessage("ASTRO", "We made the right call.", 0xFFDD3333.toInt()),
            ChatMessage("TOBAR","...", 0xFF88AACC.toInt()),
            ChatMessage("ASTRO", "Out there.", 0xFFDD3333.toInt()),
            ChatMessage("TOBAR","Yeah.", 0xFF88AACC.toInt())
        ), requiresAstroLoop = true),
        BarConversation("tb26", "pilot_astro", listOf(
            ChatMessage("ASTRO", "You called it, you know.", 0xFFDD3333.toInt()),
            ChatMessage("TOBAR","The bar?", 0xFF88AACC.toInt()),
            ChatMessage("ASTRO", "All of it.", 0xFFDD3333.toInt()),
            ChatMessage("TOBAR","Had a good feeling.", 0xFF88AACC.toInt())
        ), requiresAstroLoop = true),

        // ============================================================
        // === MULTI-WAY: 3-WAY CONVERSATIONS ===
        // ============================================================
        BarConversation(listOf("pilot_medic", "pilot_rascal", "pilot_brutus"), listOf(
            ChatMessage("MEDIC", "Rascal, did you take Brutus's wrench again?", 0xFFFF88AA.toInt()),
            ChatMessage("RASCAL", "I borrowed it. Borrowing isn't taking.", 0xFFDDAA33.toInt()),
            ChatMessage("BRUTUS", "Wrench. Now.", 0xFF77AA33.toInt()),
            ChatMessage("RASCAL", "...it's in the vents. Don't follow me.", 0xFFDDAA33.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_rascal", "pilot_frost"), listOf(
            ChatMessage("RASCAL", "Frost's locker has the good snacks. Confirmed.", 0xFFDDAA33.toInt()),
            ChatMessage("MEDIC", "Rascal, that's stealing from a crewmate.", 0xFFFF88AA.toInt()),
            ChatMessage("FROST", "It's cryo-bait. Enjoy the frostbite.", 0xFF55BBFF.toInt()),
            ChatMessage("RASCAL", "...worth it.", 0xFFDDAA33.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_brutus", "pilot_fang"), listOf(
            ChatMessage("FANG", "Someone moved through the vents last night.", 0xFF8844CC.toInt()),
            ChatMessage("RASCAL", "Wasn't me. I was asleep. In my bunk. Honest.", 0xFFDDAA33.toInt()),
            ChatMessage("BRUTUS", "Liar.", 0xFF77AA33.toInt()),
            ChatMessage("FANG", "His heartbeat agrees with Brutus.", 0xFF8844CC.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_brutus", "pilot_dash"), listOf(
            ChatMessage("DASH", "Brutus, race me! Medic can time it!", 0xFFFFDD22.toInt()),
            ChatMessage("BRUTUS", "No.", 0xFF77AA33.toInt()),
            ChatMessage("MEDIC", "I'm not timing another concussion, Dash.", 0xFFFF88AA.toInt()),
            ChatMessage("DASH", "One lap! Half a lap? A sprint?", 0xFFFFDD22.toInt())
        )),
        BarConversation(listOf("pilot_frost", "pilot_dash", "pilot_whiskers"), listOf(
            ChatMessage("DASH", "We three are the fastest on the crew, right?", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "I don't run. I deign to arrive.", 0xFFFFBB88.toInt()),
            ChatMessage("FROST", "Penguins are fast. Underwater.", 0xFF55BBFF.toInt()),
            ChatMessage("DASH", "Nobody here gets it.", 0xFFFFDD22.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_dash", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "Dash, you're limping. See the medic.", 0xFFDD3333.toInt()),
            ChatMessage("DASH", "It's a fast limp. Barely counts.", 0xFFFFDD22.toInt()),
            ChatMessage("MEDIC", "Sit. Now. Both of you stop arguing.", 0xFFFF88AA.toInt()),
            ChatMessage("ASTRO", "You heard the doctor.", 0xFFDD3333.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_ember", "pilot_fang"), listOf(
            ChatMessage("EMBER", "We three are the night's fiercest hunters!", 0xFFFF6622.toInt()),
            ChatMessage("BRUTUS", "Loud hunter. Bad hunter.", 0xFF77AA33.toInt()),
            ChatMessage("FANG", "Fire glows. I'd see you coming a mile off.", 0xFF8844CC.toInt()),
            ChatMessage("EMBER", "...noted.", 0xFFFF6622.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_ember", "pilot_whiskers"), listOf(
            ChatMessage("RASCAL", "Ember singed my stash. Again.", 0xFFDDAA33.toInt()),
            ChatMessage("EMBER", "Don't hide snacks near a phoenix.", 0xFFFF6622.toInt()),
            ChatMessage("WHISKERS", "Don't hide snacks near me either.", 0xFFFFBB88.toInt()),
            ChatMessage("RASCAL", "Nowhere is safe in this bar.", 0xFFDDAA33.toInt())
        )),
        BarConversation(listOf("pilot_frost", "pilot_fang", "pilot_kraken"), listOf(
            ChatMessage("FANG", "Cold, dark, and deep. My kind of company.", 0xFF8844CC.toInt()),
            ChatMessage("FROST", "I prefer 'serene.'", 0xFF55BBFF.toInt()),
            ChatMessage("KRAKEN", "The deep is patient. So am I.", 0xFF33AAAA.toInt()),
            ChatMessage("FROST", "Three introverts walk into a bar.", 0xFF55BBFF.toInt())
        )),
        BarConversation(listOf("pilot_dash", "pilot_ember", "pilot_havoc"), listOf(
            ChatMessage("HAVOC", "Let's blow something up! Who's in?!", 0xFFBBFF22.toInt()),
            ChatMessage("DASH", "Me! Fast! Now!", 0xFFFFDD22.toInt()),
            ChatMessage("EMBER", "I'll bring the fire.", 0xFFFF6622.toInt()),
            ChatMessage("HAVOC", "BEST. CREW. EVER.", 0xFFBBFF22.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_fang", "pilot_unit7"), listOf(
            ChatMessage("UNIT-7", "Query: why does Fang hang upside down?", 0xFF44EE55.toInt()),
            ChatMessage("FANG", "Better blood flow. Sharper senses.", 0xFF8844CC.toInt()),
            ChatMessage("MEDIC", "Medically... he's not wrong.", 0xFFFF88AA.toInt()),
            ChatMessage("UNIT-7", "Logging it as 'organic eccentricity.'", 0xFF44EE55.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_kraken", "pilot_havoc"), listOf(
            ChatMessage("HAVOC", "Strongest crew on the station, hands down!", 0xFFBBFF22.toInt()),
            ChatMessage("BRUTUS", "Hmph. Maybe.", 0xFF77AA33.toInt()),
            ChatMessage("KRAKEN", "I have eight hands. The vote is mine.", 0xFF33AAAA.toInt()),
            ChatMessage("HAVOC", "Can't argue with that math!", 0xFFBBFF22.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_whiskers", "pilot_unit7"), listOf(
            ChatMessage("UNIT-7", "Whiskers, you knocked 14 items off today.", 0xFF44EE55.toInt()),
            ChatMessage("WHISKERS", "They were in my way. All of them.", 0xFFFFBB88.toInt()),
            ChatMessage("RASCAL", "I pocketed nine before they hit the floor.", 0xFFDDAA33.toInt()),
            ChatMessage("UNIT-7", "I am surrounded by chaos agents.", 0xFF44EE55.toInt())
        )),
        BarConversation(listOf("pilot_frost", "pilot_dash", "pilot_kraken"), listOf(
            ChatMessage("KRAKEN", "The current carries those who wait.", 0xFF33AAAA.toInt()),
            ChatMessage("DASH", "Or you just swim faster. Way faster.", 0xFFFFDD22.toInt()),
            ChatMessage("FROST", "Both work. One's calmer.", 0xFF55BBFF.toInt()),
            ChatMessage("DASH", "Calm is boring.", 0xFFFFDD22.toInt())
        )),
        BarConversation(listOf("pilot_ember", "pilot_kraken", "pilot_astro"), listOf(
            ChatMessage("EMBER", "From ashes we rise!", 0xFFFF6622.toInt()),
            ChatMessage("KRAKEN", "From the depths we endure.", 0xFF33AAAA.toInt()),
            ChatMessage("ASTRO", "From the bar, we should get going.", 0xFFDD3333.toInt()),
            ChatMessage("EMBER", "...he's not wrong.", 0xFFFF6622.toInt())
        )),
        BarConversation(listOf("pilot_fang", "pilot_whiskers", "pilot_astro"), listOf(
            ChatMessage("FANG", "Two predators and a captain. Cozy.", 0xFF8844CC.toInt()),
            ChatMessage("WHISKERS", "I'm not a 'predator.' I'm a connoisseur.", 0xFFFFBB88.toInt()),
            ChatMessage("ASTRO", "You both stare at me the same way.", 0xFFDD3333.toInt()),
            ChatMessage("FANG", "We're hungry. Let's hunt.", 0xFF8844CC.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_havoc", "pilot_astro"), listOf(
            ChatMessage("MEDIC", "Havoc's in the med-bay. Seven times this week.", 0xFFFF88AA.toInt()),
            ChatMessage("HAVOC", "New record, Doc!", 0xFFBBFF22.toInt()),
            ChatMessage("ASTRO", "Havoc. Stop headbutting the ship.", 0xFFDD3333.toInt()),
            ChatMessage("HAVOC", "No promises, boss!", 0xFFBBFF22.toInt())
        )),
        BarConversation(listOf("pilot_frost", "pilot_unit7", "pilot_havoc"), listOf(
            ChatMessage("HAVOC", "Why's it so QUIET over here?!", 0xFFBBFF22.toInt()),
            ChatMessage("FROST", "It was. Until now.", 0xFF55BBFF.toInt()),
            ChatMessage("UNIT-7", "Decibel levels have spiked 400%.", 0xFF44EE55.toInt()),
            ChatMessage("HAVOC", "That's the Havoc effect!", 0xFFBBFF22.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_fang", "pilot_kraken"), listOf(
            ChatMessage("RASCAL", "Night raid on the pantry. You two in?", 0xFFDDAA33.toInt()),
            ChatMessage("FANG", "I see in the dark. I'll lead.", 0xFF8844CC.toInt()),
            ChatMessage("KRAKEN", "I'll carry everything. Eight arms.", 0xFF33AAAA.toInt()),
            ChatMessage("RASCAL", "Best crew I never deserved.", 0xFFDDAA33.toInt())
        )),
        BarConversation(listOf("pilot_dash", "pilot_whiskers", "pilot_unit7"), listOf(
            ChatMessage("DASH", "Whiskers, race me! Loser buys drinks!", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "I'm napping. The answer is napping.", 0xFFFFBB88.toInt()),
            ChatMessage("UNIT-7", "Whiskers has slept 16 hours today.", 0xFF44EE55.toInt()),
            ChatMessage("DASH", "How is that even POSSIBLE?!", 0xFFFFDD22.toInt())
        )),
        BarConversation(listOf("pilot_ember", "pilot_havoc", "pilot_astro"), listOf(
            ChatMessage("HAVOC", "Ember, let's go out in a blaze of glory!", 0xFFBBFF22.toInt()),
            ChatMessage("EMBER", "Now you speak my language!", 0xFFFF6622.toInt()),
            ChatMessage("ASTRO", "Nobody's going out in any blaze. Sit.", 0xFFDD3333.toInt()),
            ChatMessage("HAVOC", "Aww, the boss is no fun.", 0xFFBBFF22.toInt())
        )),
        BarConversation(listOf("pilot_kraken", "pilot_whiskers", "pilot_unit7"), listOf(
            ChatMessage("KRAKEN", "Patience is the ocean's deepest gift.", 0xFF33AAAA.toInt()),
            ChatMessage("UNIT-7", "Patience: inefficient, but logged as valid.", 0xFF44EE55.toInt()),
            ChatMessage("WHISKERS", "I've mastered it. Watch me ignore you.", 0xFFFFBB88.toInt()),
            ChatMessage("KRAKEN", "...impressive, actually.", 0xFF33AAAA.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_unit7", "pilot_havoc"), listOf(
            ChatMessage("UNIT-7", "Brutus, your bone density is remarkable.", 0xFF44EE55.toInt()),
            ChatMessage("BRUTUS", "Stop scanning.", 0xFF77AA33.toInt()),
            ChatMessage("HAVOC", "Scan ME! I bet I'm all muscle!", 0xFFBBFF22.toInt()),
            ChatMessage("UNIT-7", "Negative. Mostly bruises.", 0xFF44EE55.toInt())
        )),
        BarConversation(listOf("pilot_frost", "pilot_ember", "pilot_astro"), listOf(
            ChatMessage("FROST", "Ember's raising the cabin temperature again.", 0xFF55BBFF.toInt()),
            ChatMessage("EMBER", "And Frost is lowering it. Balance.", 0xFFFF6622.toInt()),
            ChatMessage("ASTRO", "Can you two balance somewhere else?", 0xFFDD3333.toInt()),
            ChatMessage("FROST", "Gladly.", 0xFF55BBFF.toInt())
        )),

        // ============================================================
        // === MULTI-WAY: 4-WAY CONVERSATIONS ===
        // ============================================================
        BarConversation(listOf("pilot_medic", "pilot_rascal", "pilot_brutus", "pilot_frost"), listOf(
            ChatMessage("MEDIC", "First four aboard. Feels like a family.", 0xFFFF88AA.toInt()),
            ChatMessage("RASCAL", "A family with great resale value.", 0xFFDDAA33.toInt()),
            ChatMessage("BRUTUS", "Touch my gear and lose a hand.", 0xFF77AA33.toInt()),
            ChatMessage("FROST", "I give it a week before someone freezes.", 0xFF55BBFF.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_rascal", "pilot_dash", "pilot_ember"), listOf(
            ChatMessage("DASH", "Race you all to the launch pad!", 0xFFFFDD22.toInt()),
            ChatMessage("RASCAL", "I'll take the shortcut. Your pockets.", 0xFFDDAA33.toInt()),
            ChatMessage("EMBER", "I'll take the scenic, flaming route.", 0xFFFF6622.toInt()),
            ChatMessage("MEDIC", "I'll take the inevitable injury report.", 0xFFFF88AA.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_frost", "pilot_fang", "pilot_kraken"), listOf(
            ChatMessage("FROST", "The quiet corner of the bar. My favorite.", 0xFF55BBFF.toInt()),
            ChatMessage("FANG", "Ours now. We claimed it.", 0xFF8844CC.toInt()),
            ChatMessage("KRAKEN", "The silent currents gather here.", 0xFF33AAAA.toInt()),
            ChatMessage("BRUTUS", "Good. Stay silent.", 0xFF77AA33.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_dash", "pilot_fang", "pilot_whiskers"), listOf(
            ChatMessage("DASH", "Fastest reflexes on the crew? Settle it!", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "Cats win reflexes. This is known.", 0xFFFFBB88.toInt()),
            ChatMessage("FANG", "I hear movement before it happens.", 0xFF8844CC.toInt()),
            ChatMessage("MEDIC", "Don't 'settle it' in my med-bay.", 0xFFFF88AA.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_ember", "pilot_kraken", "pilot_havoc"), listOf(
            ChatMessage("HAVOC", "Loudest table in the bar! Keep it up!", 0xFFBBFF22.toInt()),
            ChatMessage("EMBER", "The fire roars with you!", 0xFFFF6622.toInt()),
            ChatMessage("RASCAL", "Great cover for my exit. Thanks, boys.", 0xFFDDAA33.toInt()),
            ChatMessage("KRAKEN", "I'll be the calm in your storm.", 0xFF33AAAA.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_dash", "pilot_whiskers", "pilot_unit7"), listOf(
            ChatMessage("UNIT-7", "Four very different operating speeds here.", 0xFF44EE55.toInt()),
            ChatMessage("DASH", "I'm the fast one!", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "I'm the slow one. By choice.", 0xFFFFBB88.toInt()),
            ChatMessage("BRUTUS", "I'm leaving.", 0xFF77AA33.toInt())
        )),
        BarConversation(listOf("pilot_frost", "pilot_ember", "pilot_fang", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "Ice, fire, and a bat walk into a bar.", 0xFFDD3333.toInt()),
            ChatMessage("FROST", "There's no punchline. Just us.", 0xFF55BBFF.toInt()),
            ChatMessage("EMBER", "I'M the punchline. A bright one!", 0xFFFF6622.toInt()),
            ChatMessage("FANG", "I prefer the setup. In the dark.", 0xFF8844CC.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_kraken", "pilot_unit7", "pilot_havoc"), listOf(
            ChatMessage("MEDIC", "Checkups for all four of you. Today.", 0xFFFF88AA.toInt()),
            ChatMessage("KRAKEN", "I have three hearts. Triple the work.", 0xFF33AAAA.toInt()),
            ChatMessage("UNIT-7", "I have none. Skip me.", 0xFF44EE55.toInt()),
            ChatMessage("HAVOC", "Check mine LAST, I break the machine!", 0xFFBBFF22.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_frost", "pilot_whiskers", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "Has anyone seen my flight jacket?", 0xFFDD3333.toInt()),
            ChatMessage("RASCAL", "Nope. Never. Couldn't say.", 0xFFDDAA33.toInt()),
            ChatMessage("WHISKERS", "It's warm. I'm sitting on it.", 0xFFFFBB88.toInt()),
            ChatMessage("FROST", "Rascal lined his vents with the sleeves.", 0xFF55BBFF.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_ember", "pilot_unit7", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "Status report, all of you.", 0xFFDD3333.toInt()),
            ChatMessage("BRUTUS", "Ready.", 0xFF77AA33.toInt()),
            ChatMessage("EMBER", "Burning to launch!", 0xFFFF6622.toInt()),
            ChatMessage("UNIT-7", "All systems nominal.", 0xFF44EE55.toInt())
        )),
        BarConversation(listOf("pilot_dash", "pilot_kraken", "pilot_whiskers", "pilot_havoc"), listOf(
            ChatMessage("HAVOC", "Arm wrestle! Everybody, right now!", 0xFFBBFF22.toInt()),
            ChatMessage("KRAKEN", "I'd need a bigger table. Eight arms.", 0xFF33AAAA.toInt()),
            ChatMessage("DASH", "I'd win on speed alone!", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "I'd win by not participating.", 0xFFFFBB88.toInt())
        )),
        BarConversation(listOf("pilot_fang", "pilot_unit7", "pilot_havoc", "pilot_astro"), listOf(
            ChatMessage("UNIT-7", "Three humans and a bat. Statistically odd.", 0xFF44EE55.toInt()),
            ChatMessage("FANG", "I'm the interesting one.", 0xFF8844CC.toInt()),
            ChatMessage("HAVOC", "No way, I'M the interesting one!", 0xFFBBFF22.toInt()),
            ChatMessage("ASTRO", "You're all giving me a headache.", 0xFFDD3333.toInt())
        )),

        // ============================================================
        // === MULTI-WAY: TB-26 / TOBAR GROUPS ===
        // ============================================================
        // T1 — TB + Medic + Rascal
        BarConversation(listOf("tb26", "pilot_medic", "pilot_rascal"), listOf(
            ChatMessage("TB-26", "Rascal's tab is longer than my circuits.", 0xFF88AACC.toInt()),
            ChatMessage("RASCAL", "Put it on my tab. I'm good for it.", 0xFFDDAA33.toInt()),
            ChatMessage("MEDIC", "He is absolutely not good for it.", 0xFFFF88AA.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_medic", "pilot_rascal"), listOf(
            ChatMessage("TOBAR", "Rascal's tab is longer than my arm.", 0xFF88AACC.toInt()),
            ChatMessage("RASCAL", "Put it on my tab. I'm good for it.", 0xFFDDAA33.toInt()),
            ChatMessage("MEDIC", "He is absolutely not good for it.", 0xFFFF88AA.toInt())
        ), requiresAstroLoop = true),
        // T2 — TB + Brutus + Dash
        BarConversation(listOf("tb26", "pilot_brutus", "pilot_dash"), listOf(
            ChatMessage("DASH", "TB-26, fastest drink pour you've got!", 0xFFFFDD22.toInt()),
            ChatMessage("TB-26", "I have one speed. Reluctant.", 0xFF88AACC.toInt()),
            ChatMessage("BRUTUS", "Same.", 0xFF77AA33.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_brutus", "pilot_dash"), listOf(
            ChatMessage("DASH", "Tobar, fastest drink pour you've got!", 0xFFFFDD22.toInt()),
            ChatMessage("TOBAR", "I have one speed. Reluctant.", 0xFF88AACC.toInt()),
            ChatMessage("BRUTUS", "Same.", 0xFF77AA33.toInt())
        ), requiresAstroLoop = true),
        // T3 — TB + Frost + Havoc
        BarConversation(listOf("tb26", "pilot_frost", "pilot_havoc"), listOf(
            ChatMessage("HAVOC", "Coldest, strongest thing you can pour!", 0xFFBBFF22.toInt()),
            ChatMessage("TB-26", "Coolant. It's the only thing I pour.", 0xFF88AACC.toInt()),
            ChatMessage("FROST", "I'll take it lukewarm. Less exciting.", 0xFF55BBFF.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_frost", "pilot_havoc"), listOf(
            ChatMessage("HAVOC", "Coldest, strongest thing you can pour!", 0xFFBBFF22.toInt()),
            ChatMessage("TOBAR", "House special. Don't ask the year.", 0xFF88AACC.toInt()),
            ChatMessage("FROST", "I'll take it lukewarm. Less exciting.", 0xFF55BBFF.toInt())
        ), requiresAstroLoop = true),
        // T4 — TB + Ember + Whiskers
        BarConversation(listOf("tb26", "pilot_ember", "pilot_whiskers"), listOf(
            ChatMessage("EMBER", "Bartender! Something that burns going down!", 0xFFFF6622.toInt()),
            ChatMessage("TB-26", "I serve coolant. The opposite of that.", 0xFF88AACC.toInt()),
            ChatMessage("WHISKERS", "I'll have whatever's in the warm spot.", 0xFFFFBB88.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_ember", "pilot_whiskers"), listOf(
            ChatMessage("EMBER", "Bartender! Something that burns going down!", 0xFFFF6622.toInt()),
            ChatMessage("TOBAR", "I pour one thing. Manage your hopes.", 0xFF88AACC.toInt()),
            ChatMessage("WHISKERS", "I'll have whatever's in the warm spot.", 0xFFFFBB88.toInt())
        ), requiresAstroLoop = true),
        // T5 — TB + Kraken + Unit-7
        BarConversation(listOf("tb26", "pilot_kraken", "pilot_unit7"), listOf(
            ChatMessage("UNIT-7", "TB-26, we are both machines. Solidarity?", 0xFF44EE55.toInt()),
            ChatMessage("TB-26", "I was built to destroy. Now I garnish.", 0xFF88AACC.toInt()),
            ChatMessage("KRAKEN", "We all drift from our purpose. Drink?", 0xFF33AAAA.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_kraken", "pilot_unit7"), listOf(
            ChatMessage("UNIT-7", "Tobar, do you ever miss the old days?", 0xFF44EE55.toInt()),
            ChatMessage("TOBAR", "Every day. Then I pour another.", 0xFF88AACC.toInt()),
            ChatMessage("KRAKEN", "We all drift from our purpose. Drink?", 0xFF33AAAA.toInt())
        ), requiresAstroLoop = true),
        // T6 — TB + Fang + Rascal
        BarConversation(listOf("tb26", "pilot_fang", "pilot_rascal"), listOf(
            ChatMessage("FANG", "Your tab's in the dark corner, Rascal.", 0xFF8844CC.toInt()),
            ChatMessage("RASCAL", "How do you even see that far?", 0xFFDDAA33.toInt()),
            ChatMessage("TB-26", "He doesn't. I keep the ledger.", 0xFF88AACC.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_fang", "pilot_rascal"), listOf(
            ChatMessage("FANG", "Your tab's in the dark corner, Rascal.", 0xFF8844CC.toInt()),
            ChatMessage("RASCAL", "How do you even see that far?", 0xFFDDAA33.toInt()),
            ChatMessage("TOBAR", "He doesn't. I keep the ledger.", 0xFF88AACC.toInt())
        ), requiresAstroLoop = true),
        // T7 — TB + Medic + Astro + Havoc (4-way)
        BarConversation(listOf("tb26", "pilot_medic", "pilot_astro", "pilot_havoc"), listOf(
            ChatMessage("ASTRO", "TB-26, the usual for the table.", 0xFFDD3333.toInt()),
            ChatMessage("TB-26", "Three coolants. Garnished with regret.", 0xFF88AACC.toInt()),
            ChatMessage("HAVOC", "Make mine a DOUBLE regret!", 0xFFBBFF22.toInt()),
            ChatMessage("MEDIC", "Make his a water. He's concussed.", 0xFFFF88AA.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_medic", "pilot_astro", "pilot_havoc"), listOf(
            ChatMessage("ASTRO", "Tobar, the usual for the table.", 0xFFDD3333.toInt()),
            ChatMessage("TOBAR", "Three on the house. Garnished with regret.", 0xFF88AACC.toInt()),
            ChatMessage("HAVOC", "Make mine a DOUBLE regret!", 0xFFBBFF22.toInt()),
            ChatMessage("MEDIC", "Make his a water. He's concussed.", 0xFFFF88AA.toInt())
        ), requiresAstroLoop = true),
        // T8 — TB + Dash + Ember + Kraken (4-way)
        BarConversation(listOf("tb26", "pilot_dash", "pilot_ember", "pilot_kraken"), listOf(
            ChatMessage("DASH", "Four drinks, TB, and make it quick!", 0xFFFFDD22.toInt()),
            ChatMessage("TB-26", "Quick isn't in my programming.", 0xFF88AACC.toInt()),
            ChatMessage("EMBER", "Slow service builds the suspense!", 0xFFFF6622.toInt()),
            ChatMessage("KRAKEN", "I'll wait. The tide always returns.", 0xFF33AAAA.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_dash", "pilot_ember", "pilot_kraken"), listOf(
            ChatMessage("DASH", "Four drinks, Tobar, and make it quick!", 0xFFFFDD22.toInt()),
            ChatMessage("TOBAR", "Quick isn't in my nature.", 0xFF88AACC.toInt()),
            ChatMessage("EMBER", "Slow service builds the suspense!", 0xFFFF6622.toInt()),
            ChatMessage("KRAKEN", "I'll wait. The tide always returns.", 0xFF33AAAA.toInt())
        ), requiresAstroLoop = true),

        // ============================================================
        // === MULTI-WAY: 3-WAY CONVERSATIONS (BATCH 2) ===
        // ============================================================
        BarConversation(listOf("pilot_medic", "pilot_rascal", "pilot_dash"), listOf(
            ChatMessage("DASH", "Rascal, you're faster than you look!", 0xFFFFDD22.toInt()),
            ChatMessage("RASCAL", "Years of running from people I owe.", 0xFFDDAA33.toInt()),
            ChatMessage("MEDIC", "That tracks, medically and financially.", 0xFFFF88AA.toInt()),
            ChatMessage("DASH", "Teach me the getaway routes!", 0xFFFFDD22.toInt())
        )),
        BarConversation(listOf("pilot_frost", "pilot_brutus", "pilot_ember"), listOf(
            ChatMessage("EMBER", "Two cold souls and one eternal flame.", 0xFFFF6622.toInt()),
            ChatMessage("FROST", "You're raising my insurance premiums.", 0xFF55BBFF.toInt()),
            ChatMessage("BRUTUS", "And the room temperature.", 0xFF77AA33.toInt()),
            ChatMessage("EMBER", "Beauty has a cost.", 0xFFFF6622.toInt())
        )),
        BarConversation(listOf("pilot_fang", "pilot_kraken", "pilot_whiskers"), listOf(
            ChatMessage("FANG", "Three night-stalkers at one table.", 0xFF8844CC.toInt()),
            ChatMessage("KRAKEN", "I stalk the deep, not the dark.", 0xFF33AAAA.toInt()),
            ChatMessage("WHISKERS", "I stalk the sofa. It never escapes.", 0xFFFFBB88.toInt()),
            ChatMessage("FANG", "...we have range, as a group.", 0xFF8844CC.toInt())
        )),
        BarConversation(listOf("pilot_unit7", "pilot_havoc", "pilot_astro"), listOf(
            ChatMessage("HAVOC", "Unit-7, calculate my odds of survival!", 0xFFBBFF22.toInt()),
            ChatMessage("UNIT-7", "Low. You removed your own shields.", 0xFF44EE55.toInt()),
            ChatMessage("ASTRO", "He does that every single run.", 0xFFDD3333.toInt()),
            ChatMessage("HAVOC", "And I'm STILL here!", 0xFFBBFF22.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_frost", "pilot_fang"), listOf(
            ChatMessage("MEDIC", "Both of you run colder than a corpse.", 0xFFFF88AA.toInt()),
            ChatMessage("FROST", "Efficient circulation.", 0xFF55BBFF.toInt()),
            ChatMessage("FANG", "Less to hear when I listen for hearts.", 0xFF8844CC.toInt()),
            ChatMessage("MEDIC", "I regret asking. As always.", 0xFFFF88AA.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_dash", "pilot_kraken"), listOf(
            ChatMessage("RASCAL", "Kraken, eight arms? Think of the loot.", 0xFFDDAA33.toInt()),
            ChatMessage("KRAKEN", "I use them for balance, not burglary.", 0xFF33AAAA.toInt()),
            ChatMessage("DASH", "I'd just grab it and outrun everyone.", 0xFFFFDD22.toInt()),
            ChatMessage("RASCAL", "A man of taste.", 0xFFDDAA33.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_ember", "pilot_unit7"), listOf(
            ChatMessage("UNIT-7", "Brutus and Ember register peak aggression.", 0xFF44EE55.toInt()),
            ChatMessage("BRUTUS", "Good.", 0xFF77AA33.toInt()),
            ChatMessage("EMBER", "We are art in motion!", 0xFFFF6622.toInt()),
            ChatMessage("UNIT-7", "I will increase my distance.", 0xFF44EE55.toInt())
        )),
        BarConversation(listOf("pilot_whiskers", "pilot_havoc", "pilot_astro"), listOf(
            ChatMessage("HAVOC", "Whiskers, wanna cause some CHAOS?", 0xFFBBFF22.toInt()),
            ChatMessage("WHISKERS", "I cause chaos by doing nothing at all.", 0xFFFFBB88.toInt()),
            ChatMessage("ASTRO", "She knocked my coffee off the console.", 0xFFDD3333.toInt()),
            ChatMessage("WHISKERS", "Exactly. Effortless.", 0xFFFFBB88.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_dash", "pilot_whiskers"), listOf(
            ChatMessage("MEDIC", "Your heart rates are polar opposites.", 0xFFFF88AA.toInt()),
            ChatMessage("DASH", "Mine's redlining! Best feeling ever!", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "Mine's asleep. Like the rest of me.", 0xFFFFBB88.toInt()),
            ChatMessage("MEDIC", "One of you, please meet in the middle.", 0xFFFF88AA.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_ember", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "Rascal, did you pawn the spare thruster?", 0xFFDD3333.toInt()),
            ChatMessage("RASCAL", "Define 'pawn.' And 'spare.'", 0xFFDDAA33.toInt()),
            ChatMessage("EMBER", "He traded it for a very shiny lighter.", 0xFFFF6622.toInt()),
            ChatMessage("RASCAL", "Ember, we agreed you'd keep quiet!", 0xFFDDAA33.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_fang", "pilot_havoc"), listOf(
            ChatMessage("HAVOC", "Scariest table in the bar, no contest!", 0xFFBBFF22.toInt()),
            ChatMessage("FANG", "I don't try to be scary. I just am.", 0xFF8844CC.toInt()),
            ChatMessage("BRUTUS", "Hmph.", 0xFF77AA33.toInt()),
            ChatMessage("HAVOC", "See? Brutus agrees, loudly!", 0xFFBBFF22.toInt())
        )),
        BarConversation(listOf("pilot_frost", "pilot_kraken", "pilot_unit7"), listOf(
            ChatMessage("KRAKEN", "Stillness is the ocean's true language.", 0xFF33AAAA.toInt()),
            ChatMessage("FROST", "Finally, someone who gets it.", 0xFF55BBFF.toInt()),
            ChatMessage("UNIT-7", "Three low-energy units. Optimal.", 0xFF44EE55.toInt()),
            ChatMessage("FROST", "Was that a compliment, Unit-7?", 0xFF55BBFF.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_ember", "pilot_kraken"), listOf(
            ChatMessage("MEDIC", "Ember, stop cauterizing on Kraken's advice.", 0xFFFF88AA.toInt()),
            ChatMessage("KRAKEN", "I merely said the deep heals all wounds.", 0xFF33AAAA.toInt()),
            ChatMessage("EMBER", "I improved it with fire!", 0xFFFF6622.toInt()),
            ChatMessage("MEDIC", "Neither of you is a doctor.", 0xFFFF88AA.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_fang", "pilot_whiskers"), listOf(
            ChatMessage("RASCAL", "Three sneakiest crew members, united.", 0xFFDDAA33.toInt()),
            ChatMessage("FANG", "I hear you coming every time, Rascal.", 0xFF8844CC.toInt()),
            ChatMessage("WHISKERS", "I simply don't care enough to sneak.", 0xFFFFBB88.toInt()),
            ChatMessage("RASCAL", "Tough crowd for a heist pitch.", 0xFFDDAA33.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_havoc", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "Try not to wreck the hangar today.", 0xFFDD3333.toInt()),
            ChatMessage("BRUTUS", "No promises.", 0xFF77AA33.toInt()),
            ChatMessage("HAVOC", "We're DEMOLITION buddies now!", 0xFFBBFF22.toInt()),
            ChatMessage("ASTRO", "That's what I was afraid of.", 0xFFDD3333.toInt())
        )),
        BarConversation(listOf("pilot_frost", "pilot_dash", "pilot_unit7"), listOf(
            ChatMessage("DASH", "Bet I can lap the bar before you blink!", 0xFFFFDD22.toInt()),
            ChatMessage("UNIT-7", "I do not blink. You would lose.", 0xFF44EE55.toInt()),
            ChatMessage("FROST", "He's not exaggerating. I've watched.", 0xFF55BBFF.toInt()),
            ChatMessage("DASH", "Creepiest bet I've ever lost.", 0xFFFFDD22.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_ember", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "Ember, the medic says slow down.", 0xFFDD3333.toInt()),
            ChatMessage("EMBER", "Slowing down is a kind of dying!", 0xFFFF6622.toInt()),
            ChatMessage("MEDIC", "So is third-degree burns, dramatically.", 0xFFFF88AA.toInt()),
            ChatMessage("ASTRO", "Listen to the doctor, Ember.", 0xFFDD3333.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_frost", "pilot_whiskers"), listOf(
            ChatMessage("WHISKERS", "Someone's been in my snack stash.", 0xFFFFBB88.toInt()),
            ChatMessage("RASCAL", "Couldn't be me. I have an alibi.", 0xFFDDAA33.toInt()),
            ChatMessage("FROST", "You're holding her tuna right now.", 0xFF55BBFF.toInt()),
            ChatMessage("RASCAL", "...this is MY identical tuna.", 0xFFDDAA33.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_fang", "pilot_kraken"), listOf(
            ChatMessage("FANG", "Three of the quietest hunters aboard.", 0xFF8844CC.toInt()),
            ChatMessage("BRUTUS", "Talk less.", 0xFF77AA33.toInt()),
            ChatMessage("KRAKEN", "He communicates efficiently. I respect it.", 0xFF33AAAA.toInt()),
            ChatMessage("FANG", "...this is a good table.", 0xFF8844CC.toInt())
        )),
        BarConversation(listOf("pilot_dash", "pilot_unit7", "pilot_havoc"), listOf(
            ChatMessage("HAVOC", "Race AND explosions! Best combo ever!", 0xFFBBFF22.toInt()),
            ChatMessage("DASH", "I'm in for the racing half!", 0xFFFFDD22.toInt()),
            ChatMessage("UNIT-7", "Survival probability: declining rapidly.", 0xFF44EE55.toInt()),
            ChatMessage("HAVOC", "That's the FUN probability!", 0xFFBBFF22.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_unit7", "pilot_havoc"), listOf(
            ChatMessage("MEDIC", "Havoc, back in the med-bay already?", 0xFFFF88AA.toInt()),
            ChatMessage("HAVOC", "I high-fived a bulkhead! It lost!", 0xFFBBFF22.toInt()),
            ChatMessage("UNIT-7", "The bulkhead is undamaged. You are not.", 0xFF44EE55.toInt()),
            ChatMessage("MEDIC", "Sit down before you 'win' again.", 0xFFFF88AA.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_ember", "pilot_fang"), listOf(
            ChatMessage("RASCAL", "Night raid, you two. I need lookouts.", 0xFFDDAA33.toInt()),
            ChatMessage("FANG", "I see everything. Obviously.", 0xFF8844CC.toInt()),
            ChatMessage("EMBER", "I'll light the way!", 0xFFFF6622.toInt()),
            ChatMessage("RASCAL", "Ember, that's the OPPOSITE of stealth.", 0xFFDDAA33.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_frost", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "Two of my steadiest pilots, right here.", 0xFFDD3333.toInt()),
            ChatMessage("BRUTUS", "We hold the line.", 0xFF77AA33.toInt()),
            ChatMessage("FROST", "Calmly. And cold.", 0xFF55BBFF.toInt()),
            ChatMessage("ASTRO", "Couldn't ask for better anchors.", 0xFFDD3333.toInt())
        )),
        BarConversation(listOf("pilot_dash", "pilot_whiskers", "pilot_kraken"), listOf(
            ChatMessage("DASH", "Fastest, laziest, and... most arms. Team?", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "I contribute by supervising.", 0xFFFFBB88.toInt()),
            ChatMessage("KRAKEN", "I contribute eight of everything.", 0xFF33AAAA.toInt()),
            ChatMessage("DASH", "I'll contribute the running!", 0xFFFFDD22.toInt())
        )),

        // ============================================================
        // === MULTI-WAY: 4-WAY CONVERSATIONS (BATCH 2) ===
        // ============================================================
        BarConversation(listOf("pilot_medic", "pilot_dash", "pilot_whiskers", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "How's the crew holding up, Medic?", 0xFFDD3333.toInt()),
            ChatMessage("MEDIC", "Dash won't rest, Whiskers won't wake.", 0xFFFF88AA.toInt()),
            ChatMessage("DASH", "Resting wastes daylight!", 0xFFFFDD22.toInt()),
            ChatMessage("WHISKERS", "Waking wastes naps.", 0xFFFFBB88.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_brutus", "pilot_ember", "pilot_unit7"), listOf(
            ChatMessage("UNIT-7", "This table's chaos rating is severe.", 0xFF44EE55.toInt()),
            ChatMessage("RASCAL", "We prefer 'spirited.'", 0xFFDDAA33.toInt()),
            ChatMessage("EMBER", "We prefer 'legendary!'", 0xFFFF6622.toInt()),
            ChatMessage("BRUTUS", "We prefer quiet.", 0xFF77AA33.toInt())
        )),
        BarConversation(listOf("pilot_frost", "pilot_fang", "pilot_kraken", "pilot_havoc"), listOf(
            ChatMessage("HAVOC", "Why's this corner so creepy and quiet?!", 0xFFBBFF22.toInt()),
            ChatMessage("FROST", "Was. Past tense.", 0xFF55BBFF.toInt()),
            ChatMessage("FANG", "You're loud enough for all of us.", 0xFF8844CC.toInt()),
            ChatMessage("KRAKEN", "The deep was calmer. Marginally.", 0xFF33AAAA.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_ember", "pilot_fang", "pilot_havoc"), listOf(
            ChatMessage("MEDIC", "Three frequent fliers to my med-bay.", 0xFFFF88AA.toInt()),
            ChatMessage("EMBER", "Burns are badges!", 0xFFFF6622.toInt()),
            ChatMessage("HAVOC", "Bruises are trophies!", 0xFFBBFF22.toInt()),
            ChatMessage("FANG", "I just like the quiet beds.", 0xFF8844CC.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_frost", "pilot_unit7", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "Inventory's short again. Anyone?", 0xFFDD3333.toInt()),
            ChatMessage("RASCAL", "Shrinkage. It happens. Tragic.", 0xFFDDAA33.toInt()),
            ChatMessage("UNIT-7", "Rascal removed nine items at 0300.", 0xFF44EE55.toInt()),
            ChatMessage("FROST", "The robot keeps receipts, Rascal.", 0xFF55BBFF.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_dash", "pilot_kraken", "pilot_whiskers"), listOf(
            ChatMessage("DASH", "Strongest, slowest, armiest, and ME!", 0xFFFFDD22.toInt()),
            ChatMessage("BRUTUS", "Strength wins.", 0xFF77AA33.toInt()),
            ChatMessage("KRAKEN", "Reach wins.", 0xFF33AAAA.toInt()),
            ChatMessage("WHISKERS", "Apathy wins. I'm not even trying.", 0xFFFFBB88.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_brutus", "pilot_fang", "pilot_unit7"), listOf(
            ChatMessage("UNIT-7", "Scanning vitals. Brutus reads as 'rock.'", 0xFF44EE55.toInt()),
            ChatMessage("BRUTUS", "Accurate.", 0xFF77AA33.toInt()),
            ChatMessage("FANG", "His heartbeat's slower than mine.", 0xFF8844CC.toInt()),
            ChatMessage("MEDIC", "You're all impossible patients.", 0xFFFF88AA.toInt())
        )),
        BarConversation(listOf("pilot_rascal", "pilot_dash", "pilot_havoc", "pilot_astro"), listOf(
            ChatMessage("HAVOC", "Fastest, loudest, sneakiest, and the boss!", 0xFFBBFF22.toInt()),
            ChatMessage("DASH", "Dream team!", 0xFFFFDD22.toInt()),
            ChatMessage("RASCAL", "Speak for yourselves on 'loud.'", 0xFFDDAA33.toInt()),
            ChatMessage("ASTRO", "Why do I feel a heist coming on?", 0xFFDD3333.toInt())
        )),
        BarConversation(listOf("pilot_frost", "pilot_ember", "pilot_kraken", "pilot_whiskers"), listOf(
            ChatMessage("FROST", "Ice, fire, ocean, and a house cat.", 0xFF55BBFF.toInt()),
            ChatMessage("EMBER", "I bring the warmth!", 0xFFFF6622.toInt()),
            ChatMessage("KRAKEN", "I bring the depth.", 0xFF33AAAA.toInt()),
            ChatMessage("WHISKERS", "I bring nothing. You're all welcome.", 0xFFFFBB88.toInt())
        )),
        BarConversation(listOf("pilot_medic", "pilot_rascal", "pilot_ember", "pilot_kraken"), listOf(
            ChatMessage("MEDIC", "Vitals check, all four of you. Sit.", 0xFFFF88AA.toInt()),
            ChatMessage("RASCAL", "Can't. Allergic to sitting still.", 0xFFDDAA33.toInt()),
            ChatMessage("EMBER", "I run hot. The reading lies.", 0xFFFF6622.toInt()),
            ChatMessage("KRAKEN", "Three hearts. Be thorough.", 0xFF33AAAA.toInt())
        )),
        BarConversation(listOf("pilot_brutus", "pilot_fang", "pilot_whiskers", "pilot_havoc"), listOf(
            ChatMessage("HAVOC", "Predator power table! Let's hunt!", 0xFFBBFF22.toInt()),
            ChatMessage("FANG", "Hunting requires silence, Havoc.", 0xFF8844CC.toInt()),
            ChatMessage("WHISKERS", "And effort. Pass.", 0xFFFFBB88.toInt()),
            ChatMessage("BRUTUS", "Sit. Be quiet.", 0xFF77AA33.toInt())
        )),
        BarConversation(listOf("pilot_frost", "pilot_dash", "pilot_unit7", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "Status, all of you. Quick version.", 0xFFDD3333.toInt()),
            ChatMessage("DASH", "Quick is my ONLY version!", 0xFFFFDD22.toInt()),
            ChatMessage("FROST", "Stable. Cold. Ready.", 0xFF55BBFF.toInt()),
            ChatMessage("UNIT-7", "Systems nominal.", 0xFF44EE55.toInt())
        )),

        // ============================================================
        // === MULTI-WAY: TB-26 / TOBAR GROUPS (BATCH 2) ===
        // ============================================================
        // NT1 — TB + Frost + Brutus
        BarConversation(listOf("tb26", "pilot_frost", "pilot_brutus"), listOf(
            ChatMessage("BRUTUS", "Drink.", 0xFF77AA33.toInt()),
            ChatMessage("TB-26", "One coolant. Served with no emotion.", 0xFF88AACC.toInt()),
            ChatMessage("FROST", "My kind of service.", 0xFF55BBFF.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_frost", "pilot_brutus"), listOf(
            ChatMessage("BRUTUS", "Drink.", 0xFF77AA33.toInt()),
            ChatMessage("TOBAR", "One pour. Served with no small talk.", 0xFF88AACC.toInt()),
            ChatMessage("FROST", "My kind of service.", 0xFF55BBFF.toInt())
        ), requiresAstroLoop = true),
        // NT2 — TB + Dash + Whiskers
        BarConversation(listOf("tb26", "pilot_dash", "pilot_whiskers"), listOf(
            ChatMessage("DASH", "TB-26, fastest thing on the menu!", 0xFFFFDD22.toInt()),
            ChatMessage("TB-26", "Stillness. It's all I have on tap.", 0xFF88AACC.toInt()),
            ChatMessage("WHISKERS", "I'll take a double.", 0xFFFFBB88.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_dash", "pilot_whiskers"), listOf(
            ChatMessage("DASH", "Tobar, fastest thing on the menu!", 0xFFFFDD22.toInt()),
            ChatMessage("TOBAR", "Stillness. It's all I have on tap.", 0xFF88AACC.toInt()),
            ChatMessage("WHISKERS", "I'll take a double.", 0xFFFFBB88.toInt())
        ), requiresAstroLoop = true),
        // NT3 — TB + Ember + Kraken
        BarConversation(listOf("tb26", "pilot_ember", "pilot_kraken"), listOf(
            ChatMessage("EMBER", "Bartender, something with FIRE!", 0xFFFF6622.toInt()),
            ChatMessage("TB-26", "I extinguish. I do not ignite.", 0xFF88AACC.toInt()),
            ChatMessage("KRAKEN", "The deep cools all flames, friend.", 0xFF33AAAA.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_ember", "pilot_kraken"), listOf(
            ChatMessage("EMBER", "Bartender, something with FIRE!", 0xFFFF6622.toInt()),
            ChatMessage("TOBAR", "I cool things down. House rule.", 0xFF88AACC.toInt()),
            ChatMessage("KRAKEN", "The deep cools all flames, friend.", 0xFF33AAAA.toInt())
        ), requiresAstroLoop = true),
        // NT4 — TB + Havoc + Unit-7
        BarConversation(listOf("tb26", "pilot_havoc", "pilot_unit7"), listOf(
            ChatMessage("HAVOC", "TB, pour me something DANGEROUS!", 0xFFBBFF22.toInt()),
            ChatMessage("TB-26", "Danger isn't on my service menu.", 0xFF88AACC.toInt()),
            ChatMessage("UNIT-7", "His blood already exceeds safe limits.", 0xFF44EE55.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_havoc", "pilot_unit7"), listOf(
            ChatMessage("HAVOC", "Tobar, pour me something DANGEROUS!", 0xFFBBFF22.toInt()),
            ChatMessage("TOBAR", "Danger isn't on my service menu.", 0xFF88AACC.toInt()),
            ChatMessage("UNIT-7", "His blood already exceeds safe limits.", 0xFF44EE55.toInt())
        ), requiresAstroLoop = true),
        // NT5 — TB + Rascal + Astro
        BarConversation(listOf("tb26", "pilot_rascal", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "TB-26, settle Rascal's tab tonight.", 0xFFDD3333.toInt()),
            ChatMessage("RASCAL", "Whoa, boss, that's a generous offer.", 0xFFDDAA33.toInt()),
            ChatMessage("TB-26", "I added the items he 'forgot' to mention.", 0xFF88AACC.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_rascal", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "Tobar, settle Rascal's tab tonight.", 0xFFDD3333.toInt()),
            ChatMessage("RASCAL", "Whoa, boss, that's a generous offer.", 0xFFDDAA33.toInt()),
            ChatMessage("TOBAR", "I added the items he 'forgot' to mention.", 0xFF88AACC.toInt())
        ), requiresAstroLoop = true),
        // NT6 — TB + Medic + Fang
        BarConversation(listOf("tb26", "pilot_medic", "pilot_fang"), listOf(
            ChatMessage("FANG", "Your gears tick irregularly, TB-26.", 0xFF8844CC.toInt()),
            ChatMessage("TB-26", "I'm old. The ticking is character.", 0xFF88AACC.toInt()),
            ChatMessage("MEDIC", "I can recalibrate that, you know.", 0xFFFF88AA.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_medic", "pilot_fang"), listOf(
            ChatMessage("FANG", "Your pulse ticks irregularly, Tobar.", 0xFF8844CC.toInt()),
            ChatMessage("TOBAR", "I'm old. The ticking is character.", 0xFF88AACC.toInt()),
            ChatMessage("MEDIC", "I can recalibrate that, you know.", 0xFFFF88AA.toInt())
        ), requiresAstroLoop = true),
        // NT7 — TB + Rascal + Kraken + Whiskers (4-way)
        BarConversation(listOf("tb26", "pilot_rascal", "pilot_kraken", "pilot_whiskers"), listOf(
            ChatMessage("RASCAL", "Four-handed pours? Kraken should bartend.", 0xFFDDAA33.toInt()),
            ChatMessage("KRAKEN", "I'd never keep up with the dishes.", 0xFF33AAAA.toInt()),
            ChatMessage("WHISKERS", "TB-26 doesn't wash dishes anyway.", 0xFFFFBB88.toInt()),
            ChatMessage("TB-26", "Correct. We own no dishes.", 0xFF88AACC.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_rascal", "pilot_kraken", "pilot_whiskers"), listOf(
            ChatMessage("RASCAL", "Four-handed pours? Kraken should bartend.", 0xFFDDAA33.toInt()),
            ChatMessage("KRAKEN", "I'd never keep up with the dishes.", 0xFF33AAAA.toInt()),
            ChatMessage("WHISKERS", "Tobar doesn't wash dishes anyway.", 0xFFFFBB88.toInt()),
            ChatMessage("TOBAR", "Correct. We own no dishes.", 0xFF88AACC.toInt())
        ), requiresAstroLoop = true),
        // NT8 — TB + Frost + Dash + Astro (4-way)
        BarConversation(listOf("tb26", "pilot_frost", "pilot_dash", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "A round for my fastest and coldest.", 0xFFDD3333.toInt()),
            ChatMessage("DASH", "Make mine to go! No, to STAY! Both!", 0xFFFFDD22.toInt()),
            ChatMessage("FROST", "Mine over ice. Obviously.", 0xFF55BBFF.toInt()),
            ChatMessage("TB-26", "Everything I serve is already ice.", 0xFF88AACC.toInt())
        ), blockedInAstroLoop = true),
        BarConversation(listOf("tb26", "pilot_frost", "pilot_dash", "pilot_astro"), listOf(
            ChatMessage("ASTRO", "A round for my fastest and coldest.", 0xFFDD3333.toInt()),
            ChatMessage("DASH", "Make mine to go! No, to STAY! Both!", 0xFFFFDD22.toInt()),
            ChatMessage("FROST", "Mine over ice. Obviously.", 0xFF55BBFF.toInt()),
            ChatMessage("TOBAR", "Everything I serve is already cold.", 0xFF88AACC.toInt())
        ), requiresAstroLoop = true),

        // === END MULTI-WAY CONVERSATIONS ===
    )

    fun getShieldDiscoveryConversation(): BarConversation? {
        val rascal = PilotDefinitions.getPilot("pilot_rascal") ?: return null
        val unit7 = PilotDefinitions.getPilot("pilot_unit7") ?: return null
        val brutus = PilotDefinitions.getPilot("pilot_brutus") ?: return null
        return BarConversation(
            pilotAId = "pilot_rascal",
            pilotBId = "pilot_brutus",
            lines = listOf(
                ChatMessage(rascal.callsign, "Found a sweet shield rig on that old red wreck.", rascal.color),
                ChatMessage(unit7.callsign, "Confirmed. Military-grade. Non-standard origin.", unit7.color),
                ChatMessage(rascal.callsign, "Didn't ask where it came from. Just took it.", rascal.color),
                ChatMessage(brutus.callsign, "Reverse-engineered it. One on each ship now.", brutus.color)
            )
        )
    }

    fun getAvailable(
        unlockedPilotIds: Set<String>,
        arcCompleted: Boolean = false,
        isAstroLoop: Boolean = false
    ): List<BarConversation> {
        return conversations
            .filter { convo ->
                convo.participantIds.all { it in unlockedPilotIds || it == "tb26" } &&
                (!convo.requiresArcCompleted || arcCompleted) &&
                (!convo.blockedInAstroLoop || !isAstroLoop) &&
                (!convo.requiresAstroLoop || isAstroLoop)
            }
            .map { convo ->
                if (!isAstroLoop) convo
                else convo.copy(lines = convo.lines.map { msg ->
                    msg.copy(
                        speaker = if (msg.speaker == "TB-26") "TOBAR" else msg.speaker,
                        text = msg.text.replace("TB-26", "Tobar")
                    )
                })
            }
    }
}
