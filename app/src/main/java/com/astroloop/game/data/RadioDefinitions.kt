package com.astroloop.game.data

object RadioDefinitions {

    private val lines: Map<String, Map<String, List<String>>> = mapOf(

        // =====================================================================
        // ALWAYS-TRIGGER EVENTS (3 lines per pilot)
        // =====================================================================

        "shields_down" to mapOf(
            "pilot_medic" to listOf("Shields are down! Watch yourself!", "No more shields — be careful!", "Shield generator's offline!"),
            "pilot_rascal" to listOf("Shields popped! Time to get sneaky!", "No shields? No problem... maybe.", "We're naked out here!"),
            "pilot_brutus" to listOf("Shields gone.", "Hmph. No shields.", "Good. Now it's personal."),
            "pilot_frost" to listOf("Shield buffer depleted.", "Running without shields. Suboptimal.", "Cold comfort — shields are gone."),
            "pilot_dash" to listOf("Shields down! Gotta move faster!", "No shields — speed is our armor now!", "Lost shields! Dodge everything!"),
            "pilot_ember" to listOf("Shields burned out!", "No shields? I'll burn brighter!", "We're exposed! Light 'em up!"),
            "pilot_fang" to listOf("Shields... gone. I feel the void.", "No shields. Darkness draws closer.", "Exposed. Good. Let them come."),
            "pilot_whiskers" to listOf("Shields are gone. How annoying.", "Ugh. No shields. Beneath me.", "I did NOT sign up for this."),
            "pilot_kraken" to listOf("Shield matrix offline.", "Tentacles brace for impact.", "No shields. The deep protects."),
            "pilot_havoc" to listOf("SHIELDS DOWN! LET'S GOOO!", "Who needs shields anyway?!", "Bare hull! Maximum excitement!"),
            "pilot_unit7" to listOf("Shield status: depleted.", "Warning: shield generator offline.", "Survival odds: recalculating."),
            "pilot_astro" to listOf("Shields are down. Stay sharp.", "Lost shields — TB-26, watch our six.", "No shields. We've had worse.")
        ),

        "big_hit" to mapOf(
            "pilot_medic" to listOf(
                "Huge hit! Who's flying?!",
                "That spiked every readout!",
                "I'm a doctor, not a dummy!"
            ),
            "pilot_rascal" to listOf(
                "Whoa! Nearly lost my loot!",
                "NOT part of the heist plan!",
                "Ow! I felt that in my tail!"
            ),
            "pilot_brutus" to listOf(
                "Hngh. Hit hard.",
                "Pain. Good reminder.",
                "They'll pay for that."
            ),
            "pilot_frost" to listOf(
                "Significant hull damage. Noted.",
                "Statistically... unfortunate.",
                "Ice cracks under pressure too."
            ),
            "pilot_dash" to listOf(
                "OW! Wasn't fast enough!",
                "Big hit! Need to dodge quicker!",
                "That one rattled my teeth!"
            ),
            "pilot_ember" to listOf(
                "That blow only fans the flames!",
                "They hit hard — we'll hit harder!",
                "Pain is just fuel for the fire!"
            ),
            "pilot_fang" to listOf(
                "Pain has a certain... flavor.",
                "A heavy blow. The darkness stirs.",
                "They struck deep. I remember it."
            ),
            "pilot_whiskers" to listOf(
                "Excuse me?! Watch the ship!",
                "That was RUDE. Absolutely rude.",
                "Nine lives flashed before my eyes."
            ),
            "pilot_kraken" to listOf(
                "The hull groans like a whale song.",
                "Even leviathans feel the harpoon.",
                "A crushing blow. The abyss tests."
            ),
            "pilot_havoc" to listOf(
                "WHAT A HIT! DO IT AGAIN!",
                "HAHA! That one actually hurt!",
                "BIG DAMAGE! I'M STILL STANDING!"
            ),
            "pilot_unit7" to listOf(
                "WARNING: Hull integrity loss > 25%.",
                "Impact: critical. Evasion advised.",
                "Structural damage logged. Caution."
            ),
            "pilot_astro" to listOf(
                "Heavy hit — hull's holding. Barely.",
                "Rattled TB-26's circuits. And mine.",
                "Big impact. Shake it off."
            )
        ),

        "low_health" to mapOf(
            "pilot_medic" to listOf(
                "Hull's critical — out of bandages!",
                "Flatlining! One more hit and done!",
                "Vitals in the red — all of them!"
            ),
            "pilot_rascal" to listOf(
                "We're falling apart! Time to bail?",
                "Held together by tape and luck!",
                "One more scratch and we're junk!"
            ),
            "pilot_brutus" to listOf(
                "Hull critical. Still fighting.",
                "Bleeding out. Don't care.",
                "Almost dead. Good."
            ),
            "pilot_frost" to listOf(
                "Hull below 20%. Odds: declining.",
                "Thin ice. Thinner than usual.",
                "Critical. Don't get hit again."
            ),
            "pilot_dash" to listOf(
                "Hull's almost gone! Can't outrun!",
                "We're shredded! One hit and BOOM!",
                "Critical! Go go go go go!"
            ),
            "pilot_ember" to listOf(
                "Barely a spark — don't go out!",
                "We're a dying ember! Not like this!",
                "One breath from ashes! Fight on!"
            ),
            "pilot_fang" to listOf(
                "Close to the end. I hear it.",
                "Death is near. Its wing brushes.",
                "The hull bleeds. So fragile."
            ),
            "pilot_whiskers" to listOf(
                "Humiliating. We're barely flying.",
                "I refuse to die in this tin can.",
                "Critical hull?! Unacceptable."
            ),
            "pilot_kraken" to listOf(
                "The hull cracks. The deep calls.",
                "Sinking toward the abyss. Hold.",
                "Nearly crushed. Time to surface."
            ),
            "pilot_havoc" to listOf(
                "CRITICAL HULL! THE BEST PART!",
                "ALMOST DEAD! NEVER FELT MORE ALIVE!",
                "ONE HIT LEFT! BRING IT ON!"
            ),
            "pilot_unit7" to listOf(
                "Hull critical. Termination imminent.",
                "WARNING: One more impact = failure.",
                "Survival odds near zero. Noted."
            ),
            "pilot_astro" to listOf(
                "Hull's critical — be smart here.",
                "Hanging by a thread. TB-26, steady.",
                "One hit from done. Make it count."
            )
        ),

        "phoenix" to mapOf(
            "pilot_medic" to listOf(
                "Patient revived! Medical miracle!",
                "Flatlined and came back! Love it!",
                "Clear! We're back! Not again!"
            ),
            "pilot_rascal" to listOf(
                "Ha! Can't keep a good raccoon down!",
                "Death couldn't catch me! Slippery!",
                "Back from the dead! Got my loot!"
            ),
            "pilot_brutus" to listOf(
                "Died. Came back. Angrier.",
                "Not done yet.",
                "Death was boring. Returned."
            ),
            "pilot_frost" to listOf(
                "Resurrection complete. Bracing.",
                "Back from the cold. Colder now.",
                "Clinical death: brief. Resumed."
            ),
            "pilot_dash" to listOf(
                "I'M BACK! Too fast for the reaper!",
                "Second chance! Let's not waste it!",
                "Death couldn't slow me down!"
            ),
            "pilot_ember" to listOf(
                "RISEN FROM THE ASHES! Born to!",
                "Can't kill a phoenix! I am fire!",
                "Cinder to inferno — I LIVE AGAIN!"
            ),
            "pilot_fang" to listOf(
                "I tasted death. It tasted like me.",
                "Back from the dark. It wasn't ready.",
                "Even the void spits me out."
            ),
            "pilot_whiskers" to listOf(
                "Obviously. Not done being annoyed.",
                "Death? Please. Lives to spare.",
                "Undignified. We never speak of it."
            ),
            "pilot_kraken" to listOf(
                "The abyss returned me. Not yet.",
                "Back from the deep. Kraken endures.",
                "Death is just another ocean."
            ),
            "pilot_havoc" to listOf(
                "DIED AND CAME BACK! BEST DAY!",
                "ROUND TWO! HAHAHAHA!",
                "DEATH CAN'T HOLD ME! NOTHING CAN!"
            ),
            "pilot_unit7" to listOf(
                "Reboot complete. Systems nominal.",
                "ERROR: Death log. Status: override.",
                "Termination experienced. Resuming."
            ),
            "pilot_astro" to listOf(
                "We're back! TB-26, diagnostics!",
                "Phoenix core saved us. Still in it.",
                "Back from the edge. Stay sharp."
            )
        ),

        "boss_spawn" to mapOf(
            "pilot_medic" to listOf(
                "Massive hostile! Prep for trauma!",
                "That thing's huge! Need supplies!",
                "Boss on scanners! My heart!"
            ),
            "pilot_rascal" to listOf(
                "Big shiny target! Good loot bet!",
                "Boss incoming! Biggest score yet!",
                "That's a BIG one! More to steal!"
            ),
            "pilot_brutus" to listOf(
                "Big one. Good.",
                "Boss. Finally, a challenge.",
                "Mine."
            ),
            "pilot_frost" to listOf(
                "Large hostile. Calculating weak pts.",
                "Boss-class. Precision required.",
                "Thermal signature: enormous."
            ),
            "pilot_dash" to listOf(
                "BOSS! It's huge! Can I outrun it?!",
                "Big target! Gotta be quick!",
                "That thing is MASSIVE! Let's GO!"
            ),
            "pilot_ember" to listOf(
                "A worthy pyre! Let it BURN!",
                "Bigger they are, brighter they burn!",
                "Finally! A flame worthy of legend!"
            ),
            "pilot_fang" to listOf(
                "Something vast stirs in the dark.",
                "A great shadow. I welcome it.",
                "I can hear its heartbeat. So loud."
            ),
            "pilot_whiskers" to listOf(
                "A giant ugly thing. My favorite.",
                "A boss? Tedious. Get it over with.",
                "Grotesque. Destroy it quickly."
            ),
            "pilot_kraken" to listOf(
                "A fellow leviathan. Respect.",
                "Something massive surfaces. Stirs.",
                "A titan. Let our tentacles clash."
            ),
            "pilot_havoc" to listOf(
                "BOSS FIGHT!! YES YES YES!",
                "BIG TARGET! MAXIMUM FIREPOWER!",
                "FINALLY SOMETHING WORTH SHOOTING!"
            ),
            "pilot_unit7" to listOf(
                "Boss-class. Threat: extreme.",
                "Large hostile. Combat protocols on.",
                "Calculating strategy for target."
            ),
            "pilot_astro" to listOf(
                "Boss incoming. TB-26, weapons hot.",
                "Big one on scope. Stay focused.",
                "Boss class contact. Take it apart."
            )
        ),

        "evolution" to mapOf(
            "pilot_medic" to listOf(
                "Evolved! THAT'S good medicine!",
                "Evolved! Prognosis: terminal!",
                "Full evolution! Prescribing this!"
            ),
            "pilot_rascal" to listOf(
                "Ooh, shiny new upgrade! Jackpot!",
                "Evolved! Worth a fortune now!",
                "Look at that! Best heist haul ever!"
            ),
            "pilot_brutus" to listOf(
                "Stronger now. Good.",
                "Evolved. They're done.",
                "More power. Less talking."
            ),
            "pilot_frost" to listOf(
                "Evolution. Efficiency: dramatic.",
                "Weapon output now optimal. Elegant.",
                "Evolved. The math is beautiful."
            ),
            "pilot_dash" to listOf(
                "EVOLVED! We're unstoppable now!",
                "SO COOL! Let's use it!",
                "Evolution! Faster! Stronger! MORE!"
            ),
            "pilot_ember" to listOf(
                "EVOLUTION! The flame ascends!",
                "Reborn in fire! A BLAZE of power!",
                "The crucible forges greatness!"
            ),
            "pilot_fang" to listOf(
                "Evolved. I feel its hunger sharpen.",
                "Darker. Deadlier. Perfect.",
                "Its whisper is now a scream."
            ),
            "pilot_whiskers" to listOf(
                "Finally, worthy of my talent.",
                "Evolved. About time.",
                "Acceptable. This will do nicely."
            ),
            "pilot_kraken" to listOf(
                "Deep power flows through it.",
                "A kraken sheds its skin. Evolved.",
                "From the depths, true power rises."
            ),
            "pilot_havoc" to listOf(
                "EVOLVED! THIS THING IS INSANE!",
                "MAXIMUM POWER! HAHAHAHA!",
                "EVOLUTION! NOTHING SURVIVES THIS!"
            ),
            "pilot_unit7" to listOf(
                "Evolution complete. Output +347%.",
                "EVOLUTION. We are now the threat.",
                "Output is now... impressive."
            ),
            "pilot_astro" to listOf(
                "Evolved! TB-26, see these numbers?",
                "Full evolution. Now we're cooking.",
                "Evolved and locked in. Finish this."
            )
        ),

        // =====================================================================
        // CHANCE-TRIGGER EVENTS (2 lines per pilot)
        // =====================================================================

        "first_weapon" to mapOf(
            "pilot_medic" to listOf(
                "New weapon! Good medicine!",
                "Nice pickup! More options for us."
            ),
            "pilot_rascal" to listOf(
                "First grab! Finders keepers!",
                "Snagged one! Heist is off right."
            ),
            "pilot_brutus" to listOf(
                "Got a weapon. Let's go.",
                "More guns. Good."
            ),
            "pilot_frost" to listOf(
                "First weapon online. Damage curves?",
                "Interesting. Running the numbers."
            ),
            "pilot_dash" to listOf(
                "New weapon! Double the dakka!",
                "Armed up! How fast does it fire?!"
            ),
            "pilot_ember" to listOf(
                "First spark! Set the sky ablaze!",
                "A weapon! The flame begins small."
            ),
            "pilot_fang" to listOf(
                "First weapon. The night hunt begins.",
                "Armed now. The darkness has teeth."
            ),
            "pilot_whiskers" to listOf(
                "A weapon. Finally. I was bored.",
                "Acceptable. This one has potential."
            ),
            "pilot_kraken" to listOf(
                "Like a tentacle finding its grip.",
                "Armed. The deep provides."
            ),
            "pilot_havoc" to listOf(
                "FIRST WEAPON! TIME TO PARTY!",
                "ARMED AND DANGEROUS! LET'S GO!"
            ),
            "pilot_unit7" to listOf(
                "Weapon integrated. Efficiency up.",
                "Secondary weapon. Arsenal growing."
            ),
            "pilot_astro" to listOf(
                "First weapon's in. TB-26, warm up.",
                "Locked and loaded. Here we go."
            )
        ),

        "weapon_maxed" to mapOf(
            "pilot_medic" to listOf(
                "Maxed! Full dose of destruction!",
                "Level five! Healthy weapon!"
            ),
            "pilot_rascal" to listOf(
                "Maxed! Worth top dollar!",
                "Fully upgraded! Shiniest ever!"
            ),
            "pilot_brutus" to listOf(
                "Maxed. Good enough.",
                "Full power. Smash."
            ),
            "pilot_frost" to listOf(
                "Maximum efficiency. Satisfying.",
                "Peak performance. Numbers: pristine."
            ),
            "pilot_dash" to listOf(
                "MAXED! It's so fast now!",
                "Level five! Can it go higher?!"
            ),
            "pilot_ember" to listOf(
                "Maximum power! A SUPERNOVA!",
                "Maxed out! The forge burns eternal!"
            ),
            "pilot_fang" to listOf(
                "Maximum level. A perfect predator.",
                "Fully honed. Fangs in the dark."
            ),
            "pilot_whiskers" to listOf(
                "Maxed. I suppose that's adequate.",
                "Level five. Took long enough."
            ),
            "pilot_kraken" to listOf(
                "Fully grown. Tentacle at full reach.",
                "Maximum depth achieved. Magnificent."
            ),
            "pilot_havoc" to listOf(
                "MAXED OUT! UNLIMITED POWER!",
                "FULL LEVEL! MORE DAKKA!"
            ),
            "pilot_unit7" to listOf(
                "Upgrade ceiling. No further gains.",
                "Level 5. Performance: optimal."
            ),
            "pilot_astro" to listOf(
                "Maxed — that's our big gun now.",
                "Level five. TB-26, flag it primary."
            )
        ),

        "passive_maxed" to mapOf(
            "pilot_medic" to listOf(
                "Passive maxed! Best treatment plan!",
                "Full stacks! Preventive care!"
            ),
            "pilot_rascal" to listOf(
                "Passive maxed! Pockets stuffed!",
                "All stacks! Cleared the vault!"
            ),
            "pilot_brutus" to listOf(
                "Maxed. Stronger.",
                "Full stacks. Good."
            ),
            "pilot_frost" to listOf(
                "Fully stacked. Optimal config.",
                "Max stacks. Efficiency plateaus."
            ),
            "pilot_dash" to listOf(
                "Passive maxed! Cranked up!",
                "Full stacks! I FEEL the boost!"
            ),
            "pilot_ember" to listOf(
                "Maxed! Inner flame burns brightest!",
                "Full stacks — a fire fully kindled!"
            ),
            "pilot_fang" to listOf(
                "Quiet power, fully awakened.",
                "All stacks. Shadows deepen."
            ),
            "pilot_whiskers" to listOf(
                "Maxed. Could've happened sooner.",
                "Full stacks. I deserve this much."
            ),
            "pilot_kraken" to listOf(
                "Fully saturated. Deep current flows.",
                "Max stacks. Pressure of the deep."
            ),
            "pilot_havoc" to listOf(
                "PASSIVE MAXED! STACKED UP!",
                "FULL STACKS! FEELS INCREDIBLE!"
            ),
            "pilot_unit7" to listOf(
                "Passive at max. No further upgrades.",
                "All slots filled. Efficiency: peak."
            ),
            "pilot_astro" to listOf(
                "Passive maxed. Every bit helps.",
                "Full stacks on that one. Solid."
            )
        ),

        "first_enemy" to mapOf(
            "pilot_medic" to listOf(
                "Enemy ships! Here come patients.",
                "Enemies! Don't get hurt — two hands!"
            ),
            "pilot_rascal" to listOf(
                "Company! Bet they've got stuff!",
                "Enemies! Time to pick pockets!"
            ),
            "pilot_brutus" to listOf(
                "Enemies. Finally.",
                "Targets. Good."
            ),
            "pilot_frost" to listOf(
                "Enemies detected. Cold protocol.",
                "Enemy contact. Calculating."
            ),
            "pilot_dash" to listOf(
                "ENEMIES! Here they come! I'm READY!",
                "Enemy ships! Let's race 'em!"
            ),
            "pilot_ember" to listOf(
                "Enemies! Fresh kindling!",
                "Ships inbound — the pyre awaits!"
            ),
            "pilot_fang" to listOf(
                "Prey. I hear their engines tremble.",
                "Ships in the dark. Unaware of me."
            ),
            "pilot_whiskers" to listOf(
                "Oh, enemies. How original.",
                "Enemies. Can't even fly in peace."
            ),
            "pilot_kraken" to listOf(
                "Ships on surface. Drag them down.",
                "Like fish above my domain."
            ),
            "pilot_havoc" to listOf(
                "ENEMIES! FINALLY SOMEONE TO SHOOT!",
                "HERE THEY COME! OPEN FIRE!"
            ),
            "pilot_unit7" to listOf(
                "Hostiles detected. Weapons: free.",
                "Enemy contact. Combat routines on."
            ),
            "pilot_astro" to listOf(
                "First wave. TB-26, heads up.",
                "Enemies on scope. Handle this."
            )
        ),

        "density_spike" to mapOf(
            "pilot_medic" to listOf(
                "Too many! Can't treat them all!",
                "Swarm! This is a triage nightmare!"
            ),
            "pilot_rascal" to listOf(
                "They're everywhere! Getting spicy!",
                "Swarming! Hard to pickpocket!"
            ),
            "pilot_brutus" to listOf(
                "More targets. More punching.",
                "Swarm. Bring it."
            ),
            "pilot_frost" to listOf(
                "Density spiking. Math: unpleasant.",
                "Enemy count up. Odds: unfavorable."
            ),
            "pilot_dash" to listOf(
                "SO MANY! Gotta weave through!",
                "Everywhere! Dodging at max speed!"
            ),
            "pilot_ember" to listOf(
                "A swarm! More fuel for the inferno!",
                "They crowd the sky — burn them ALL!"
            ),
            "pilot_fang" to listOf(
                "The swarm thickens. A feast.",
                "So many heartbeats. So many targets."
            ),
            "pilot_whiskers" to listOf(
                "Absurd. No personal space here.",
                "Swarming me?! No manners?!"
            ),
            "pilot_kraken" to listOf(
                "They school together. Easy prey.",
                "A swarm. The kraken feeds."
            ),
            "pilot_havoc" to listOf(
                "SO MANY TARGETS! THIS IS PARADISE!",
                "THEY'RE EVERYWHERE! I LOVE IT!"
            ),
            "pilot_unit7" to listOf(
                "Density exceeds parameters.",
                "ALERT: Target-rich environment."
            ),
            "pilot_astro" to listOf(
                "Getting thick. TB-26, stay close.",
                "Enemy count ramping up — stay sharp."
            )
        ),

        "yen_milestone" to mapOf(
            "pilot_medic" to listOf(
                "A fortune! Enough for a hospital!",
                "Massive haul! Insurance can't match!"
            ),
            "pilot_rascal" to listOf(
                "ALL THAT YEN! Biggest score ever!",
                "We're RICH! I'm just holding it."
            ),
            "pilot_brutus" to listOf(
                "Lots of yen. Buy more ammo.",
                "Rich. Don't care. Keep fighting."
            ),
            "pilot_frost" to listOf(
                "Impressive yen. Cold, hard fortune.",
                "Profit margins: exceptional."
            ),
            "pilot_dash" to listOf(
                "SO MUCH YEN! Ka-ching ka-ching!",
                "We're making bank at top speed!"
            ),
            "pilot_ember" to listOf(
                "Golden fortune forged in fire!",
                "Yen overflowing! Wealth like smoke!"
            ),
            "pilot_fang" to listOf(
                "The dark respects fortune.",
                "Wealth whispers in the void."
            ),
            "pilot_whiskers" to listOf(
                "Acceptable earnings. I might stay.",
                "Finally, worthy compensation."
            ),
            "pilot_kraken" to listOf(
                "Treasure from the deep. Hoarded.",
                "A sea of yen. Sunken gold."
            ),
            "pilot_havoc" to listOf(
                "WE'RE LOADED! SPEND IT ALL ON GUNS!",
                "LOOK AT THAT YEN! KABOOM MONEY!"
            ),
            "pilot_unit7" to listOf(
                "Yen exceeds projections. Noted.",
                "Yen milestone. Efficiency: notable."
            ),
            "pilot_astro" to listOf(
                "Serious haul. Drinks on me later.",
                "TB-26, log that — best payday yet."
            )
        ),

        // =====================================================================
        // COMBAT START (always triggers once at game start)
        // =====================================================================

        "combat_start" to mapOf(
            "pilot_medic" to listOf("Let's keep everyone alive out there."),
            "pilot_rascal" to listOf("Time to grab some loot! Let's go!"),
            "pilot_brutus" to listOf("Finally."),
            "pilot_frost" to listOf("Engines cold. Weapons hot. Go."),
            "pilot_dash" to listOf("GO GO GO GO GO!"),
            "pilot_ember" to listOf("Light the engines. Light everything."),
            "pilot_fang" to listOf("Into the dark. Where I belong."),
            "pilot_whiskers" to listOf("Fine. Let's get this over with."),
            "pilot_kraken" to listOf("The deep calls. We answer."),
            "pilot_havoc" to listOf("FULL POWER! NO REGRETS!"),
            "pilot_unit7" to listOf("Combat systems online. Launching."),
            "pilot_astro" to listOf("Ready when you are, TB-26.")
        ),

        // =====================================================================
        // NEAR MISS — reflexive dodge reactions (2-3 lines per pilot)
        // =====================================================================

        "near_miss" to mapOf(
            "pilot_medic" to listOf(
                "Too close! My heart can't take this!",
                "Almost clipped us! Need a breather.",
                "Whew! Barely avoided a house call!"
            ),
            "pilot_rascal" to listOf(
                "Ha! Missed me! Too slippery!",
                "Whoa! Almost nicked my tail!",
                "Nice try! Can't catch this raccoon!"
            ),
            "pilot_brutus" to listOf(
                "Missed.",
                "Close. Not close enough.",
                "Tch. Sloppy shot."
            ),
            "pilot_frost" to listOf(
                "Margin: negligible. Noted.",
                "That was within two meters. Tight.",
                "Close call. Statistically expected."
            ),
            "pilot_dash" to listOf(
                "WHOA! Too close! Faster, FASTER!",
                "Almost got us! Need more speed!",
                "That grazed us! My reflexes rule!"
            ),
            "pilot_ember" to listOf(
                "Try harder than that!",
                "Close! That one singed us!",
                "Barely dodged — flame won't flinch!"
            ),
            "pilot_fang" to listOf(
                "I felt that one whisper past.",
                "Close. Death brushed my wing.",
                "Almost. But I'm the predator here."
            ),
            "pilot_whiskers" to listOf(
                "Excuse me?! Watch where you shoot!",
                "How dare they. Far too close.",
                "Hmph. My reflexes are impeccable."
            ),
            "pilot_kraken" to listOf(
                "It passed like a current. Close.",
                "The deep flinched. A rare thing.",
                "That one grazed the tentacles."
            ),
            "pilot_havoc" to listOf(
                "WHOA! THAT WAS AWESOME!",
                "ALMOST GOT ME! DO IT AGAIN!",
                "HAHA! CLOSE ONE! I LOVE THIS!"
            ),
            "pilot_unit7" to listOf(
                "Near miss. Margin: 1.3 meters.",
                "Evasion successful. Recalculating.",
                "Projectile passed within tolerance."
            ),
            "pilot_astro" to listOf(
                "Close one. Stay sharp, TB-26.",
                "That nearly clipped us. Eyes open.",
                "Tight dodge. Keep it together."
            )
        ),

        // =====================================================================
        // KILL STREAK — 3+ rapid kills, celebratory (2-3 lines per pilot)
        // =====================================================================

        "kill_streak" to mapOf(
            "pilot_medic" to listOf(
                "Hostiles down! No one to treat!",
                "Streak! Triage just got easier!",
                "Three down! No patients lost!"
            ),
            "pilot_rascal" to listOf(
                "Combo! Look at all that loot!",
                "Triple kill! This is a gold mine!",
                "On a roll! Best heist ever!"
            ),
            "pilot_brutus" to listOf(
                "More. Keep 'em coming.",
                "Streak. Good.",
                "Three down. Not enough."
            ),
            "pilot_frost" to listOf(
                "Kill efficiency spiking. Impressive.",
                "Multiple kills. Data looks good.",
                "Streak confirmed. Cold and precise."
            ),
            "pilot_dash" to listOf(
                "BOOM BOOM BOOM! Can't stop us!",
                "Kill streak! On FIRE! Speed kills!",
                "Three down! Who's next?!"
            ),
            "pilot_ember" to listOf(
                "A chain of fire! One by one!",
                "Kill streak! The blaze spreads!",
                "One after another — inferno!"
            ),
            "pilot_fang" to listOf(
                "The hunt is bountiful tonight.",
                "Three prey, three kills. Efficient.",
                "A streak of shadow. Unseen."
            ),
            "pilot_whiskers" to listOf(
                "Naturally. I make this look easy.",
                "Streak. As expected of my caliber.",
                "Three down. Yawn. Next?"
            ),
            "pilot_kraken" to listOf(
                "They fall like ships into the deep.",
                "A feast of wreckage. Kraken feeds.",
                "Three dragged down. More follow."
            ),
            "pilot_havoc" to listOf(
                "KILL STREAK! HAHAHA YES!",
                "THREE DOWN! KEEP IT GOING!",
                "STREAK! EXPLOSIONS EVERYWHERE!"
            ),
            "pilot_unit7" to listOf(
                "Multi-kill. Efficiency: optimal.",
                "Streak active. Combat rating: up.",
                "Sequential eliminations confirmed."
            ),
            "pilot_astro" to listOf(
                "Three down fast. We're in the zone.",
                "Streak — TB-26, keep the pressure.",
                "On a roll. Don't let up now."
            )
        ),

        // =====================================================================
        // TIME MILESTONES — survival timestamps (1 line per pilot)
        // =====================================================================

        "time_2min" to mapOf(
            "pilot_medic" to listOf("Two minutes in. Vitals look stable."),
            "pilot_rascal" to listOf("Two minutes! Just warming up."),
            "pilot_brutus" to listOf("Two minutes. Waiting for a fight."),
            "pilot_frost" to listOf("Two minutes. Baseline established."),
            "pilot_dash" to listOf("Two minutes! Just getting started!"),
            "pilot_ember" to listOf("Two minutes. Flame's just catching."),
            "pilot_fang" to listOf("Two minutes. Hunt's barely begun."),
            "pilot_whiskers" to listOf("Two minutes. Tolerable, I suppose."),
            "pilot_kraken" to listOf("Two minutes. Still in the shallows."),
            "pilot_havoc" to listOf("TWO MINUTES! JUST WARMING UP!"),
            "pilot_unit7" to listOf("2:00. All systems nominal."),
            "pilot_astro" to listOf("Two minutes. Settling in nicely.")
        ),

        "time_4min" to mapOf(
            "pilot_medic" to listOf("Four minutes! No casualties yet!"),
            "pilot_rascal" to listOf("Four minutes! Loot's piling up!"),
            "pilot_brutus" to listOf("Four minutes. Getting good now."),
            "pilot_frost" to listOf("Four minutes. Survival curve: good."),
            "pilot_dash" to listOf("Four minutes and still flying fast!"),
            "pilot_ember" to listOf("Four minutes — fire burns steady."),
            "pilot_fang" to listOf("Four minutes in the dark. Good."),
            "pilot_whiskers" to listOf("Four minutes. Better than most."),
            "pilot_kraken" to listOf("Four minutes. The current carries."),
            "pilot_havoc" to listOf("FOUR MINUTES OF PURE CHAOS! MORE!"),
            "pilot_unit7" to listOf("4:00. Performance within parameters."),
            "pilot_astro" to listOf("Four minutes. Solid run, TB-26.")
        ),

        "time_6min" to mapOf(
            "pilot_medic" to listOf("Six minutes — getting hairy!"),
            "pilot_rascal" to listOf("Six minutes! Dicey — I love it!"),
            "pilot_brutus" to listOf("Six minutes. Now it's a real fight."),
            "pilot_frost" to listOf("Six minutes. Threat density: rising."),
            "pilot_dash" to listOf("Six minutes! Getting crazy out here!"),
            "pilot_ember" to listOf("Six minutes — crucible burns hotter!"),
            "pilot_fang" to listOf("Six minutes. Darker prey now."),
            "pilot_whiskers" to listOf("Six minutes. I'm slightly impressed."),
            "pilot_kraken" to listOf("Six minutes. Pressure of the deep."),
            "pilot_havoc" to listOf("SIX MINUTES! MAYHEM NEVER STOPS!"),
            "pilot_unit7" to listOf("6:00. Threat escalation: notable."),
            "pilot_astro" to listOf("Six minutes. Stay focused, TB-26.")
        ),

        "time_8min" to mapOf(
            "pilot_medic" to listOf("Eight minutes! We're tough!"),
            "pilot_rascal" to listOf("Eight minutes! Longest heist ever!"),
            "pilot_brutus" to listOf("Eight minutes. Respect."),
            "pilot_frost" to listOf("Eight minutes. Top percentile now."),
            "pilot_dash" to listOf("EIGHT MINUTES! We're LEGENDS!"),
            "pilot_ember" to listOf("Eight minutes — forged in fire!"),
            "pilot_fang" to listOf("Eight minutes. Few survive this."),
            "pilot_whiskers" to listOf("Eight minutes. I'll admit — not bad."),
            "pilot_kraken" to listOf("Eight minutes in the abyss. Home."),
            "pilot_havoc" to listOf("EIGHT MINUTES! UNSTOPPABLE!"),
            "pilot_unit7" to listOf("8:00. Longevity: exceptional."),
            "pilot_astro" to listOf("Eight minutes. Veteran run, TB-26.")
        ),

        // =====================================================================
        // ASTRO LOOP EXTENDED MILESTONES (indices 4-7, Astro Loop mode only)
        // =====================================================================

        "time_10min" to mapOf(
            "pilot_medic"    to listOf("Ten minutes. Still no casualties."),
            "pilot_rascal"   to listOf("Ten minutes! Am I setting records?"),
            "pilot_brutus"   to listOf("Ten minutes. We belong here."),
            "pilot_frost"    to listOf("Ten minutes. Survival curve: strong."),
            "pilot_dash"     to listOf("TEN MINUTES! Still going! FASTER!"),
            "pilot_ember"    to listOf("Ten minutes. Fire doesn't tire."),
            "pilot_fang"     to listOf("Ten minutes. The hunt endures."),
            "pilot_whiskers" to listOf("Ten minutes. I'll allow it."),
            "pilot_kraken"   to listOf("Ten minutes. The current holds."),
            "pilot_havoc"    to listOf("TEN MINUTES! CHAOS FOREVER!"),
            "pilot_unit7"    to listOf("10:00. Output sustained. Confirmed."),
            "pilot_astro"    to listOf("Ten minutes. Not slowing down.")
        ),

        "time_12min" to mapOf(
            "pilot_medic"    to listOf("Twelve minutes. Outstanding."),
            "pilot_rascal"   to listOf("Twelve minutes! Getting legendary."),
            "pilot_brutus"   to listOf("Twelve minutes. Few reach this."),
            "pilot_frost"    to listOf("Twelve minutes. Statistical outlier."),
            "pilot_dash"     to listOf("TWELVE MINUTES! History, baby!"),
            "pilot_ember"    to listOf("Twelve minutes. Flame holds steady."),
            "pilot_fang"     to listOf("Twelve minutes. Only apex remains."),
            "pilot_whiskers" to listOf("Twelve minutes. Genuinely impressed."),
            "pilot_kraken"   to listOf("Twelve minutes. Uncharted current."),
            "pilot_havoc"    to listOf("TWELVE MINUTES! WE OWN THE SKY!"),
            "pilot_unit7"    to listOf("12:00. Outside all normal ranges."),
            "pilot_astro"    to listOf("Twelve minutes. I know this place.")
        ),

        "time_14min" to mapOf(
            "pilot_medic"    to listOf("Fourteen minutes. Something special."),
            "pilot_rascal"   to listOf("Fourteen minutes. Even I'm in awe."),
            "pilot_brutus"   to listOf("Fourteen minutes. Never been here."),
            "pilot_frost"    to listOf("Fourteen minutes. No data for this."),
            "pilot_dash"     to listOf("FOURTEEN MINUTES! Nobody else!"),
            "pilot_ember"    to listOf("Fourteen minutes. Eternal flame."),
            "pilot_fang"     to listOf("Fourteen minutes. The apex of hunts."),
            "pilot_whiskers" to listOf("Fourteen minutes. This is different."),
            "pilot_kraken"   to listOf("Fourteen minutes. Past any chart."),
            "pilot_havoc"    to listOf("FOURTEEN MINUTES! NEVER STOPPING!"),
            "pilot_unit7"    to listOf("14:00. Precedent: none found."),
            "pilot_astro"    to listOf("Fourteen minutes. This is mine.")
        ),

        "time_16min" to mapOf(
            "pilot_medic"    to listOf("Sixteen minutes. We made it count."),
            "pilot_rascal"   to listOf("Sixteen minutes. Can't top this."),
            "pilot_brutus"   to listOf("Sixteen minutes. I've seen it now."),
            "pilot_frost"    to listOf("Sixteen minutes. This rewrites data."),
            "pilot_dash"     to listOf("SIXTEEN MINUTES. This is the run."),
            "pilot_ember"    to listOf("Sixteen minutes. Now we're legend."),
            "pilot_fang"     to listOf("Sixteen minutes. I am the apex."),
            "pilot_whiskers" to listOf("Sixteen minutes. Even I'm at a loss."),
            "pilot_kraken"   to listOf("Sixteen minutes. The deep is home."),
            "pilot_havoc"    to listOf("SIXTEEN MINUTES. ...yeah. That's it."),
            "pilot_unit7"    to listOf("16:00. Run defies classification."),
            "pilot_astro"    to listOf("Sixteen minutes. This is enough.")
        ),

        "astro_echo" to mapOf(
            "pilot_medic" to listOf("Ten minutes. Something feels off."),
            "pilot_rascal" to listOf("Ten minutes. This is weird, right?"),
            "pilot_brutus" to listOf("Ten minutes. Air's gone heavy."),
            "pilot_frost" to listOf("Ten minutes. Something's shifted."),
            "pilot_dash" to listOf("Ten minutes. You feel that too?"),
            "pilot_ember" to listOf("Ten minutes. The air's wrong."),
            "pilot_fang" to listOf("Ten minutes. My instincts itch."),
            "pilot_whiskers" to listOf("Ten minutes. My fur's on end."),
            "pilot_kraken" to listOf("Ten minutes. The current's odd."),
            "pilot_havoc" to listOf("Ten minutes. Somethin's buzzin'."),
            "pilot_unit7" to listOf("10:00. Anomaly. Source unknown."),
            "pilot_astro" to listOf("Ten minutes. Like I've been here.")
        ),

        // =====================================================================
        // POST-HORROR ALTERNATES (desertCompleted && !hasDesertGoodEnding)
        // =====================================================================

        "combat_start_horror" to mapOf(
            "pilot_medic"    to listOf("Keep everyone alive. Again."),
            "pilot_rascal"   to listOf("Feels familiar out here..."),
            "pilot_frost"    to listOf("Same field. Different run."),
            "pilot_brutus"   to listOf("Again."),
            "pilot_dash"     to listOf("Here we go. Again."),
            "pilot_ember"    to listOf("Some ashes don't stay cold."),
            "pilot_fang"     to listOf("Something follows us out here."),
            "pilot_whiskers" to listOf("Again. How quaint."),
            "pilot_kraken"   to listOf("The deep remembers."),
            "pilot_havoc"    to listOf("AGAIN! ...wait. LET'S GO!"),
            "pilot_unit7"    to listOf("Run log: incrementing."),
            "pilot_astro"    to listOf("Let's do this right.")
        ),

        "time_8min_horror" to mapOf(
            "pilot_medic"    to listOf("Eight minutes. Watch carefully."),
            "pilot_rascal"   to listOf("Eight minutes. Something's coming."),
            "pilot_frost"    to listOf("Eight minutes. Deviation incoming."),
            "pilot_brutus"   to listOf("Eight minutes. It comes."),
            "pilot_dash"     to listOf("Eight minutes! Here it comes!"),
            "pilot_ember"    to listOf("Eight minutes. The fire changes."),
            "pilot_fang"     to listOf("Eight minutes. The dark shifts."),
            "pilot_whiskers" to listOf("Eight minutes. Familiar."),
            "pilot_kraken"   to listOf("Eight minutes. The abyss stirs."),
            "pilot_havoc"    to listOf("EIGHT MINUTES! IT ALWAYS\u2014"),
            "pilot_unit7"    to listOf("8:00. Prior run data matches."),
            "pilot_astro"    to listOf("Eight minutes. Something's here.")
        ),

        // =====================================================================
        // RETREAT HOME \u2014 Astro Loop mode, pilot heads back to base
        // =====================================================================

        "retreat_home" to mapOf(
            "pilot_medic" to listOf(
                "Taking damage \u2014 pulling back.",
                "I need to get this looked at.",
                "Retreating. Back in one piece."
            ),
            "pilot_rascal" to listOf(
                "Not dying today. See ya at the bar!",
                "Live to scam another day.",
                "I'm out. Don't miss me too much."
            ),
            "pilot_brutus" to listOf(
                "Falling back. This ain't over.",
                "Taking hits. Pulling out.",
                "I'm out. Regrouping."
            ),
            "pilot_frost" to listOf(
                "Retreating. Conditions unfavorable.",
                "Pulling back. Not ideal.",
                "I'm done here. For now."
            ),
            "pilot_dash" to listOf(
                "I'm gone \u2014 full burn south!",
                "Peeling off! Full speed!",
                "Shield's up, burning it south!"
            ),
            "pilot_ember" to listOf(
                "Pulling out before this gets worse.",
                "Engines still hot \u2014 heading home.",
                "I hate running. But I'm running."
            ),
            "pilot_fang" to listOf(
                "The hunt resumes later.",
                "I choose when I die. Not today.",
                "Not here. I'm gone."
            ),
            "pilot_kraken" to listOf(
                "Pulling back. The deep calls.",
                "Diving out. Not done yet.",
                "I'm surfacing. This continues."
            ),
            "pilot_whiskers" to listOf(
                "Out of here before my luck runs dry!",
                "Used one of my nine lives just now.",
                "That was too close. I'm gone."
            ),
            "pilot_unit7" to listOf(
                "Emergency retreat initiated.",
                "Disengaging. Returning to base.",
                "Shield active. Pulling back."
            ),
            "pilot_havoc" to listOf(
                "This is NOT how I go out!",
                "Leaving \u2014 on my own terms.",
                "Retreat. But I'll be back."
            ),
            "pilot_astro" to listOf(
                "Not done. Just not here.",
                "Pulling back. This isn't over.",
                "Falling back. I'll finish this."
            )
        )
    )

    fun getLines(pilotId: String, eventType: String, filterTb26: Boolean = false): List<String>? {
        val eventLines = lines[eventType] ?: return null
        val pilotLines = eventLines[pilotId] ?: return null
        if (filterTb26 && pilotId == "pilot_astro") {
            val filtered = pilotLines.filter { "TB-26" !in it }
            if (filtered.isNotEmpty()) return filtered
            // All lines mention TB-26 — use solo fallback (may be null for some events)
            return soloAstroLines[eventType]
        }
        return pilotLines
    }

    fun getLine(pilotId: String, eventType: String, filterTb26: Boolean = false): String? =
        getLines(pilotId, eventType, filterTb26)?.randomOrNull()

    // Solo Astro lines for events where all normal lines reference TB-26
    private val soloAstroLines = mapOf(
        "combat_start" to listOf("Systems green. Let's fly."),
        "time_4min" to listOf("Four minutes. Good run so far."),
        "time_6min" to listOf("Six minutes. Stay focused."),
        "time_8min" to listOf("Eight minutes. Veteran ground.")
    )
}
