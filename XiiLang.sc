/*
possible changes:
effectDict should be global and initialized by *initClass, does not need to be evaluated on CmdPeriod

implement *all to replace XiiLangSingletone

eliminate number from nodeproxy naming


*/

// use "~/Documents/ixilang".standardizePath as the path for projects

/*

GPL license (c) thor magnusson - ixi audio, 2009-2012

// XiiLang.new("project", "key") - "project" stands for the name of the folder where soundfiles are kept. in this folder a keyMapping.ixi file can be found that maps letters to sounds. if there is no file, then the mapping will be random

TODO: Add parameter effect control
TODO: Check the use of String:drop(1) and String:drop(-1)


*/

// new in ixi lang v3

// midiout
// midiclients
// languages (you can write in your native language, or create your own language semantics for ixi lang)
// fractional time multiplication
// snapshots store and register effect states
// matrix
// coder (keys)
// store and load sessions
// suicide hotline added (you can now change your mind, in case performance has picked up)
// automatic code writing (autocode 4)
// future now works in bars as well as in seconds
// added multiplication of sustain (1242~8) - here the whole part note (1) is 8 times as long
// amplitude control added as a verb (can be used with future then:   future 1:10 >> (( john    - which would fade the agent john down)
// adding scheme-like functions for post score arguments - also working with future
// microtonal transposition in melodic mode
// intoxicants
// added XiiLangGUI() for mapping keys and for starting in .app mode
// added savescore and play score (to repeat performances)
// added panning arguments as postfix
// Add color customisation to GUI
// Recording sound file functionality
// Added pitch argument (in MIDI degrees) for rhythmic and concrete modes
// Synthdefs added to the mapping file (no distinction between samples and synths)
// scheme-like operators [+ agentname 12]
// added full stops as equivalent to spaces

// new in v4

// gui method for getting the gui
// bugfix: kill kills routines as well
// added a "replace" method that replaces items in scores with new values
// added an "insert" method that adds items in scores with new values
// added a "remove" method that removes items in scores
// added intoxicants for effecting the agents' musical timing ("hash", "beer", "coffee", "LSD", "detox") (this is experimental)
// added @ (times) postfix arg (@2) will play the score twice
// added metaagents (xx -> ~ agA 2 agB 1 ~) will play agentA twice and agentB once
// added groups as part of meta agents, so a parallel sequence of a group can be inserted into a meta agent
// changed @ to % in morph mode
// changed ~ to _ in the (duration postfix arguments - so what was (1~4) becomes (1_4))
// added support for BenoitLib networked clocks (Thanks Patrick B)
// multichannel support thanks to John Thompson (although still maintainting the stereo ixi lang, for users who use Pan2 in their synthdefs)
// live recording into buffers (using fn+Enter)
// tap timing (using ctrl + "," and "." (the "<" and ">" buttons)




// FIXED BUG: snapshots do not perk up agents that have been dozed
// FIXED BUG: Snapshots do not recall the effect state
// FIXED BUG: single character percussive mode agents wont >shift
// FIXED BUG: future bug
// FIXED BUG: the active agent is found by comparing strings in dict and on doc. (in case of many agents on doc with same name)
// FIXED BUG: location of cursor when above agent and using future
// FIXED BUG: nap and perk would not work in concrete mode
// FIXED BUG: implemented Panning in all synthdefs (was neglected in some)


// Add: ptpd support for netclocks
// �sudo ./ptpd -cd
// �http://sourceforge.net/projects/ptpd/develop

/*
Dependencies:
TempoClock:sync (by f0)
MIDIClockOut (Crucial Lib -> for slaving other apps to ixi lang)
ixiViews (for MIDIKeyboard in the XiiLangGUI)
*/

// add slide (from Array helpfile)

// TODO: add a "kill invisible" function that silences all agents that have been deleted from the score
// TODO: add a "reorgan/inject" function that injects new chars in percussive mode
// TODO: how about visualising the actions of the user in ixi lang. Data visualise it. Compare the automation and user data.
// TODO: Use autuconductor to apply actions to different agents.

// TODO: think about NRT rendering of playscores

// TODO: make a note that a particular project does not exist and that the new project was created.




// TODO NOW : fix repeats in groups (DONE, but check)
// - make all synthdefs panable (Pan2 & PanAz)
// Freeall bug, recursion not working properly (try this code):
/*
XiiLang(txt:true)

ag1 -> |b d cc cb  f  cb|@1

ag2 -> |  c  c       cc |@1

ag3 -> xylo[     1   5 2  1 ]@1
ag4 -> wood[        211  5  ]@1

shake cc

group gr1 -> ag3 ag4

ma1 -> ~ ag1 1 gr1 1 ~

// when freeing the below, there is an error
ma2 -> ~ ag1 1 ma1 1 ~

*/



XiiLang {
	classvar globaldocnum, <>useHomeDir = false;
	var <>doc, <docnum, <doccolor, <oncolor, activecolor, offcolor, deadcolor, proxyspace, groups, score;
	var <agentDict, instrDict, ixiInstr, effectDict, varDict, snapshotDict, scoreArray, metaAgentDict;
	var scale, tuning, chosenscale, tonic;
	var midiclient, <inbus, eventtype, suicidefork;
	var langCommands, englishCommands, language, english;
	var matrixArray, initargs; // matrix vars
	var projectname, key, randomseed;
	var time, tempo, tapping = false, tapcount = 0;
	var thisversion = 4;
	var numChan;
	var win;

	*new { arg project="default", keyarg="C", txt=false, newdoc=false, language, dicts, score, numChannels=2;
		^super.new.initXiiLang( project, keyarg, txt, newdoc, language, dicts, score, numChannels);
	}

	*ixiDir {
		^ if(useHomeDir) {
			Platform.userHomeDir +/+ "/Documents/SuperCollider Support/ixilang"
		} {
			Platform.userAppSupportDir +/+ "ixilang"
		}
	}

	projectDir { ^(XiiLang.ixiDir +/+ projectname) }

	initXiiLang {arg project, keyarg, txt, newdoc, lang, dicts, score, numChannels;
	//	"project, keyarg, txt, newdoc, lang, dicts, score, numChannels".postln;
	//	[project, keyarg, txt, newdoc, lang, dicts, score, numChannels].postln;
		if(score.isNil, {
			randomseed = 1000000.rand;
		},{
			randomseed = score[0];
		});
		thisThread.randSeed = randomseed;

		if(globaldocnum.isNil, {
			globaldocnum = 0;
		}, {
			globaldocnum = globaldocnum+1;
		});
		key = keyarg;

		numChan = numChannels;
		initargs = [project, key, txt, newdoc, lang, numChan];
		XiiLangSingleton.new(this, globaldocnum);
		// the number of this document (allows for multiple docs using same variable names)
		docnum = globaldocnum; // this number is now added in front of all agent names
		chosenscale = Scale.major; // this is scale representation
		scale = chosenscale.semitones.copy.add(12); // this is in degrees
		tuning = \et12; // default tuning
		tonic = 60 + [\C, \CS, \D, \DS, \E, \F, \FS, \G, \GS, \A, \AS, \B].indexOf(key.toUpper.asSymbol); // midinote 60 is the default
		inbus = 8;

		if(dicts.isNil, {
			agentDict = IdentityDictionary.new; // dicts are sent from "load"
			metaAgentDict = (); //
			varDict = IdentityDictionary.new;
			snapshotDict = IdentityDictionary.new; // dicts are sent from "load"
			groups = ();
			scoreArray = []; // the score (timeline of the performance)
		},{
			agentDict = dicts[0]; // dicts are sent from "load"
			varDict = IdentityDictionary.new;
			snapshotDict = dicts[1]; // dicts are sent from "load"
			groups = dicts[2];
			docnum = dicts[3];
		});
		TempoClock.default.tempo = 120/60;
		projectname = project;

		midiclient = 0;

		try{ // try necessary, since a user without MIDI IAC busses reported that the next two lines gave errors
			MIDIClient.init;
			// midiclient = MIDIOut(0, MIDIClient.destinations[0].uid);
			midiclient = MIDIOut(0);
		};

		eventtype = \note;
		this.makeEffectDict; // in a special method, as it has to be called after every cmd+dot
		this.envirSetup( txt, newdoc, project );
		ixiInstr = XiiLangInstr.new(project, numChannels: numChan);
		instrDict = ixiInstr.makeInstrDict;
		SynthDescLib.read;
		proxyspace = ProxySpace.new.know_(true);

		CmdPeriod.add({
			16.do({arg i; try{midiclient.allNotesOff(i)} });
			this.makeEffectDict;
			groups = ();
			agentDict = IdentityDictionary.new;
			metaAgentDict = ();
			//varDict = IdentityDictionary.new; // I might uncomment this line
			snapshotDict = IdentityDictionary.new;
			proxyspace = ProxySpace.new.know_(true);
			scoreArray = [];
		});
		englishCommands = ["group", "sequence", "future", "snapshot", "->", "))", "((", "|", "[", "{", "~", ")",
				"$", ">>", "<<", "tempo", "scale", "scalepush", "tuning", "tuningpush", "remind", "help",
				"tonality", "instr", "tonic", "grid", "kill", "doze", "perk", "nap", "shake", "swap", "replace",
				"insert", "remove", ">shift", "<shift", "invert", "expand", "revert", "up", "down", "yoyo",
				"order", "dict", "store", "load", "midiclients", "midiout", "matrix", "autocode", "coder", "twitter",
				"+", "-", "*", "/", "!", "^", "(", "<", "@", "hash", "beer", "coffee", "LSD", "detox", "new", "gui",
				"savescore", "playscore", "suicide", "hotline", "newrec", "input"];  // removed "." XXX

		if(lang.isNil, {
			english = true; // might not need this;
			langCommands = englishCommands;
		}, {
			english = false;
			language = XiiLangDicts.getDict(lang.asSymbol);
			langCommands = XiiLangDicts.getList(lang.asSymbol);
		});

//		[\langCommands, langCommands].postln;

		if(score.isNil.not, {
			this.playScore(score[1]);
		});

	}

	makeEffectDict { // more to come here + parameter control - for your own effects, simply add a new line to here and it will work out of the box
		effectDict = IdentityDictionary.new;
		effectDict[\reverb] 	= {arg sig; (sig*0.6)+FreeVerb.ar(sig, 0.85, 0.86, 0.3)};
		effectDict[\reverbL] 	= {arg sig; (sig*0.6)+FreeVerb.ar(sig, 0.95, 0.96, 0.7)};
		effectDict[\reverbS] 	= {arg sig; (sig*0.6)+FreeVerb.ar(sig, 0.45, 0.46, 0.2)};
		effectDict[\delay]  	= {arg sig; sig + AllpassC.ar(sig, 1, 0.15, 1.3 )};
		effectDict[\lowpass] 	= {arg sig; RLPF.ar(sig, 1000, 0.2)};
		effectDict[\tremolo]	= {arg sig; (sig * SinOsc.ar(2.1, 0, 5.44, 0))*0.5};
		effectDict[\vibrato]	= {arg sig; PitchShift.ar(sig, 0.008, SinOsc.ar(2.1, 0, 0.11, 1))};
		effectDict[\techno] 	= {arg sig; RLPF.ar(sig, SinOsc.ar(0.1).exprange(880,12000), 0.2)};
		effectDict[\technosaw] 	= {arg sig; RLPF.ar(sig, LFSaw.ar(0.2).exprange(880,12000), 0.2)};
		effectDict[\distort] 	= {arg sig; (3111.33*sig.distort/(1+(2231.23*sig.abs))).distort*0.02};
		effectDict[\cyberpunk]	= {arg sig, ratio = 4.5, chunk = 5; Squiz.ar(sig, ratio, chunk, 0.1)};
		effectDict[\bitcrush]	= {arg sig; Latch.ar(sig, Impulse.ar(11000*0.5)).round(0.5 ** 6.7)};
		effectDict[\antique]	= {arg sig; LPF.ar(sig, 1700) + Dust.ar(7, 0.6)};
	}

	//set up document and the keydown control
	envirSetup { arg txt, newdoc, project;
		var recdoc, path;

		/*
		//GUI.cocoa;
		//if(newdoc, {
		//	doc = Document.new;
		//}, {
		//	doc = Document.current;
		//});

		// color scheme
		try{ // check if the color file exists
			#doccolor, oncolor, activecolor, offcolor, deadcolor = Object.readArchive(Platform.userAppSupportDir ++"/ixilang/"++project++"/colors.ixi")
		};
		if(doccolor.isNil, {
			doccolor = Color.white;
			oncolor = Color.black;
			activecolor = Color.green;//Color.yellow;
			offcolor = Color.blue; //Color.green;
			deadcolor = Color.red;
		});
		win = Window.new("ixi lang   -   project :" + project.quote + "  -   window nr:" + docnum.asString);
		win.bounds_(Rect(400,300, 1000, 600));
		win.front;
		doc = TextView(win.asView, Rect(10, 10, 980,580)).focus(true);
		doc.enterInterpretsSelection = true;
		//doc.setBackground(doccolor);
		//doc.setStringColor(oncolor);
		if(txt == false, { doc.setString("") });
		win.name_("ixi lang   -   project :" + project.quote + "  -   window nr:" + docnum.asString);
		doc.setFont(Font("Monaco",20));
		//doc.promptToSave_(false);

		*/
		if(newdoc, {
			//doc = Document.new;
			doc = TextView.new(Window.new("", Rect(100, 100, 600, 700)).front, Rect(0, 0, 600, 700)).resize_(5);
		}, {
			//doc = Document.current;
			doc = TextView.new(Window.new("", Rect(100, 100, 600, 700)).front, Rect(0, 0, 600, 700)).resize_(5);
		});

		// color scheme
		try{ // check if the color file exists
			#doccolor, oncolor, activecolor, offcolor, deadcolor = Object.readArchive(this.projectDir++"/colors.ixi")
		};
		if(doccolor.isNil, {
			doccolor = Color.black;
			oncolor = Color.white;
			activecolor = Color.yellow;
			offcolor = Color.green;
			deadcolor = Color.red;
		});

		//doc.parent.bounds_(Rect(400,300, 1000, 600));
		doc.background_(doccolor);
		doc.parent.name_("ixi lang   -   project :" + project.quote + "  -   window nr:" + docnum);
		doc.font_(Font("Monaco", 16));
		if(txt == false, { doc.string_("
use (alt/option)-(arrow right) to evaluate ixilang text.
help
remind
instr
agent1 -> string[1 2    3     4]
agent2 -> |abcdefghijklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQRSTUVWXYZ|
agent3 -> d{9      8 2   1}@1\n\n\n") },
		{
			doc.string_(txt);
		}
		);
		doc.setProperty(\styleSheet, "color:white"); // set the cursor to white
		doc.setStringColor(oncolor, 0, 100000); // then set the text color to whatever the user has specified


		doc.keyDownAction_({|doc, char, mod, unicode, keycode |
			var string;
			var returnVal = nil;
			//[mod, keycode, unicode].postln;
			if( mod.isAlt &&
				((keycode==124)||(keycode==123)||(keycode==125)||(keycode==126)||
					(keycode==111)||(keycode==113)||(keycode==114)||(keycode==116)||
					(keycode==37)||(keycode==38)||(keycode==39)||(keycode==40)
				),
				{ // alt + left or up or right or down arrow keys
				"eval".postln;
				//linenr = doc.string[..doc.selectionStart-1].split($\n).size;
				//doc.selectLine(linenr);
				string = doc.selectedString; // ++ "\n";

				(string.size < 1).if({"Hilight some text!".warn});

				// alt + left
				if((keycode==123) || (keycode==37), { // not 124, 125,
					this.freeAgent(string);
				}, {
					this.opInterpreter(string);
				});
				returnVal = true;
			});
			// create a live sampler doc (fn+Enter or alt+Enter)
			if((mod.isFun || mod.isAlt) && (unicode == 3), {
				ixiInstr.createRecorderDoc(this, numChan);
				returnVal = true;
			});
			// tempo tap function (ctrl+, for starting and ctrl+. for stopping (the < and > keys))
			if(mod.isCtrl && (unicode == 44), {
				if(tapping == false, {
					time = Main.elapsedTime;
					tapping = true;
				});
				tapcount = tapcount + 1;
			});
			if(mod.isCtrl && (unicode == 46), {
				time = Main.elapsedTime - time;
				tempo = tapcount / time;
				tapping = false;
				tapcount = 0;
				" --->   ixi lang Tempo : set to % BPM\n".postf(tempo*60);
				TempoClock.default.tempo = tempo;
			});
			returnVal
		});
		doc.keyUpAction_({|doc, char, mod, unicode, keycode |
			if(mod.isFun, {
				recdoc.close;
			});
		});
		doc.onClose_({
			// xxx free buffers
			ixiInstr.freeBuffers; // not good as playscore reads a new doc (NEED TO FIX)
			proxyspace.end(4);  // free all proxies
			agentDict.do({arg agent;
				agent[2].stop;
				agent[2] = nil;
			});
			snapshotDict[\futures].stop;
		});
		// these two folders are created if the user is downloading from github and hasn't created the ixilang folder in their SC folder
		if((path = XiiLang.ixiDir).pathMatch==[]) {
			File.mkdir(path);
			("ixi-lang NOTE: an ixi lang folder was not found" + path + "was created").postln;
		};
		if((path = XiiLang.ixiDir++ "/default").pathMatch==[]) {
			File.mkdir(path);
			("ixi-lang NOTE: an ixi lang default folder was not found." + path + "was created").postln;
		};
		// just make sure that all folders are in place
	if((this.projectDir++"/scores").pathMatch==[], {
			File.mkdir(this.projectDir++"/scores");
			"ixi-lang NOTE: a scores folder was not found for saving scores - It was created".postln;
		});
	if((this.projectDir++"/recordings").pathMatch==[], {
			File.mkdir(this.projectDir++"/recordings");
	//		("mkdir -p" + (this.projectDir++"/recordings")).unixCmd; // create the recordings folder
			"ixi-lang NOTE: a recordings folder was not found for saving scores - It was created".postln;
		});
		if((this.projectDir++"/sessions").pathMatch==[], {
			File.mkdir(this.projectDir++"/sessions");
			// ("mkdir -p" + (this.projectDir++"/sessions")).unixCmd; // create the sessions folder
			"ixi-lang NOTE: a sessions folder was not found for saving scores - It was created".postln;
		});
		if((this.projectDir++"/samples").pathMatch==[], {
			File.mkdir(this.projectDir++"/samples");
			//("mkdir -p" + (this.projectDir++"/samples")).unixCmd; // create the samples folder
			"ixi-lang NOTE: a samples folder was not found for saving scores - It was created".postln;
		});
	}

	// the interpreter of thie ixi lang
	opInterpreter {arg string, return=false;
		var oldop, operator; // the various operators of the language
		var methodFound = false;
		//string = string.reject({ |c| c.ascii == 78 }); // get rid of char return XXX TESTING (before Helsinki GIG)


		scoreArray = scoreArray.add([Main.elapsedTime, string]); // recording the performance

		operator = block{|break| // better NOT mess with the order of the following... (operators using -> need to be before "->")
			langCommands.do({arg op; var suggestedop, space;
				var c = string.find(op);
				if(c.isNil.not, {
					space = string[c..string.size].find(" "); // allowing for longer operators with same beginning as shorter
					if(space.isNil.not, {
						suggestedop = string[c..(c+(space-1))];
					},{
						suggestedop = op;
					});
					if(suggestedop.size == op.size, { // this is a bit silly (longer op always has to be after the short)
						methodFound = true;
						break.value(op);
					});
				});
			});
			if(methodFound == false, {" --->   ixi lang error : OPERATOR NOT FOUND !!!".postln; });
		};

		if(english.not, { // this is the only place of non-english code apart from in the init function
			oldop = operator;
			operator = try{ language[oldop.asSymbol] } ? operator; // if operator exists in the lang dict, else use the given
			operator = operator.asString;
			string = string.replace(oldop, operator);
		});

		switch(operator)
			{"dict"}{ // TEMP: only used for debugging
				"-- Groups : ".postln;
				Post << groups; "\n".postln;

				"-- agentDicts : ".postln;
				Post << agentDict.keys; "\n".postln;
				Post << agentDict; "\n".postln;

				"-- snapshotDicts : ".postln;
				Post << snapshotDict; "\n".postln;

				"-- proxyspace : ".postln;
				Post << proxyspace; "\n".postln;

				"-- metaAgentDict : ".postln;
				Post << metaAgentDict; "\n".postln;


		//		"-- scoreArray : ".postln;
		//		Post << scoreArray; "\n".postln;

		//		"-- buffers : ".postln;
		//		Post << ixiInstr.returnBufferDict; "\n".postln;

			}
			{"store"}{
				var sessionstart, sessionend, session;
				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string.replace("  ", " ");
				string = string++" "; // add a space in order to find end of agent (if no argument)
				sessionstart = string.findAll(" ")[0];
				sessionend = string.findAll(" ")[1];
				session = string[sessionstart+1..sessionend-1];
				[projectname, key, language, doc.string, agentDict, snapshotDict, groups, docnum].writeArchive(this.projectDirname++"/sessions/"++session++".ils");
			}
			{"load"}{
				var sessionstart, sessionend, session, key, language, project;
				var tempAgentDict, tempSnapshotDict;
				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string.replace("  ", " ");
				string = string++" "; // add a space in order to find end of agent (if no argument)
				sessionstart = string.findAll(" ")[0];
				sessionend = string.findAll(" ")[1];
				session = string[sessionstart+1..sessionend-1];
				doc.onClose; // call this to end all agents and groups in this particular doc if it has been running agents
				#project, key, language, string, agentDict, snapshotDict, groups, docnum = Object.readArchive(this.projectDirname++"/sessions/"++session++".ils");
				doc.string = string;
				XiiLang.new( project, key, true, false, language, [agentDict, snapshotDict, groups, docnum]);
			}
			{"snapshot"}{
				this.parseSnapshot(string);
			}
			{"->"}{
				var mode;
				mode = block{|break|
					["|", "[", "{", ")", "$", "~"].do({arg op, i;
					    var c = string.find(op);
						if(c.isNil.not, {break.value(i)});
					});
				};
			[\mode, mode].postln;
				switch(mode)
					{0} { ^this.parseScoreMode0(string, return) }
					{1} { ^this.parseScoreMode1(string, return) }
					{2} { ^this.parseScoreMode2(string, return) }
					{3} { this.parseChord(string, \c) }
					{4} { this.parseChord(string, \n) }
					{5} { this.createMetaAgent(string) };
			}
			{"future"}{
				// future 8:4 >> swap thor // every 8 SECONDS the action is performed (here 4 times)
				// future 4b:4 >> swap thor // every 8 BARS the action is performed (here 4 times)
				// future << thor // cancel the scheduling
				var command, commandstart, colon, seconds, times, agent, pureagent, agentstart, agentend, argument;
				var cursorPos, testcond, snapshot, barmode, argumentarray, tempstring;

				string = string.reject({ |c| c.ascii == 10 }); // get rid of char return
				// allow for some sloppyness in style
				string = string++" "; // add a space in order to find end of agent (if no argument)
				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string.replace("  ", " ");

				if(string.contains(">>"), { // setting future event(s)
					commandstart = string.find(">");
					colon = string.find(":");
					if(string[6..colon-1].contains("b") || string[6..colon-1].contains("B"), {
						barmode = true;
						seconds = string[6..colon-1].tr($b, \).asFloat; // remove the b and get the beats
					},{
						barmode = false; // is the future working in seconds or bars ?
						seconds = string[6..colon-1].asFloat;
					});
					times = string[colon+1..commandstart-1].asInteger;
					if(string[commandstart+3..commandstart+10] == "snapshot", { // it's the "choose snapshot" future
						if(snapshotDict.size > 1, {
						snapshotDict[\futures].stop; // futures is when future is choosing a random snapshot
						snapshotDict[\futures] = // don't need to store the name, just a unique name
							{
								times.do({arg i;
									seconds.wait;
									testcond = false;
									while({ testcond.not }, {
										snapshot = snapshotDict.keys.choose;
										if(snapshot != \futures, { testcond = true });
									});
									{
									cursorPos = doc.selectionStart; // get cursor pos
									this.parseSnapshot("snapshot " ++ snapshot.asString);
									//doc.selectRange(cursorPos); // set cursor pos again
									doc.select(cursorPos,0); // xxx
									}.defer;
								})
							}.fork(TempoClock.new) // not using default clock, since new tempo affects wait (strange?/bug?)
						}, {
							" ---> ixi lang: you haven't stored more than one snapshot!".postln;
						});
					}, {
						agentstart = string.findAll(" ")[3];
						agentend = string.findAll(" ")[4];
						pureagent = string[agentstart+1..agentend-1];
						agent = (this.agentPrefix++pureagent).asSymbol;	// !!!
						command = string[commandstart+3..agentstart-1];
						argument = string[agentend..string.size-1];
						argumentarray = [];
						tempstring = "";
						argument.do({arg item;
							if(item.isAlphaNum || (item == $_), {
								tempstring = tempstring ++ item;
							}, {
								if(tempstring != "", {argumentarray = argumentarray.add(tempstring)}); // removed .asInteger here  XXX !!! could cause bugs
								tempstring ="";
							});
						});
						if(argumentarray == [], {argumentarray = [argument]}); // removed .asInteger here XXX !!! could cause bugs

						//if future argument contains many args then put them into a list that will be wrappedAt in the future task

						if(groups.at(agent).isNil.not, { // the "agent" is a group so we need to set routine to each of the agents in the group
							groups.at(agent).do({arg agentx, i;
								{ var agent; // this var and the value statement is needed to individualise the agents in the future
								agent = (this.agentPrefix++agentx).asSymbol;
								agentDict[agent][2] = agentDict[agent][2].add(
										{
											times.do({arg i;
												if(barmode, {
												      ((seconds*agentDict[agent][1].durarr.sum)/TempoClock.default.tempo).wait;
												},{
												      seconds.wait;
												});
												{
												scoreArray = scoreArray.add([Main.elapsedTime, command+agentx+argumentarray.wrapAt(i).asString]);
												this.parseMethod(command+agentx+argumentarray.wrapAt(i).asString); // do command
												}.defer;
											});
										}.fork(TempoClock.new)
								);
								}.value;
							});
						}, { // it is a real agent, not a group, then we ADD
							agentDict[agent][2] = agentDict[agent][2].add(
								{
									times.do({arg i;
										if(barmode, {
										      ((seconds*agentDict[agent][1].durarr.sum)/TempoClock.default.tempo).wait;
										},{
										      seconds.wait;
										});
										{
										scoreArray = scoreArray.add([Main.elapsedTime, command+pureagent+argumentarray.wrapAt(i).asString]);
										this.parseMethod(command+pureagent+argumentarray.wrapAt(i).asString); // do command
										}.defer;
									});
									// agentDict[agent][2] = nil; // set it back to nil after routine is finished
								}.fork(TempoClock.new)
							);
						});
					});
				}, { // removing future scheduling (syntax: future << agent)
					agentstart = string.findAll(" ")[1];
					agentend = string.findAll(" ")[2];
					agent = string[agentstart+1..agentend-1];
					agent = (this.agentPrefix++agent).asSymbol;
					if(groups.at(agent).isNil.not, { // the "agent" is a group so we need to set routine to each of the agents in the group
						groups.at(agent).do({arg agentx, i;
							agent = (this.agentPrefix++agentx).asSymbol;
							agentDict[agent][2].do({arg routine; routine.stop});
							agentDict[agent][2] = [];
						});
					},{
						agentDict[agent][2].do({arg routine; routine.stop});
						agentDict[agent][2] = [];
					});
				});
			}
			{">>"}{
				this.initEffect(string);
			}
			{"<<"}{
				this.removeEffect(string);
			}
			{"))"}{
				this.increaseAmp(string);
			}
			{"(("}{
				this.decreaseAmp(string);
			}
			{"tempo"}{
				var newtemp, time, op;
				var nrstart = string.find("o")+1;
				if(string.contains(":"), {
					string = string.tr($ , \);
					op = string.find(":");
					newtemp = string[nrstart..op].asInteger/60;
					time = string[op+1..string.size-1].asInteger;
					TempoClock.default.sync(newtemp, time);
				}, {
					newtemp = string[nrstart+1..string.size-1].asInteger / 60;
					TempoClock.default.tempo = newtemp;
				});
				"---> Setting tempo to : ".post; (newtemp*60).postln;
			}
			{"scale"}{
				var agent, firstarg;
				string = string++" ";
				firstarg = string[string.findAll(" ")[0]+1..(string.findAll(" ")[1])-1];
				agent = (this.agentPrefix++firstarg).asSymbol;
				if(agentDict.keys.includes(agent), {
					this.parseMethod(string); // since future will use this, I need to parse the method
				}, {
					chosenscale = ("Scale."++firstarg).interpret;
					chosenscale.tuning_(tuning.asSymbol);
					scale = chosenscale.semitones.copy.add(12); // used to be degrees, but that doesn't support tuning
				});
			}
			{"scalepush"}{
				var scalestart, scalestr;
				var dictscore, mode;
				scalestart = string.find("h");
				scalestr = string.tr($ , \);
				scalestr = scalestr[scalestart+1..scalestr.size-1];
				chosenscale = ("Scale."++scalestr).interpret;
				chosenscale.tuning_(tuning.asSymbol);
				scale = chosenscale.semitones.copy.add(12); // used to be degrees, but that doesn't support tuning
				agentDict.do({arg agent;
					dictscore = agent[1].scorestring;
					dictscore = dictscore.reject({ |c| c.ascii == 34 }); // get rid of quotation marks
					mode = block{|break|
						["|", "[", "{", ")"].do({arg op, i;						var c = dictscore.find(op);
							if(c.isNil.not, {break.value(i)});
						});
					};
					switch(mode)
						{0} { this.parseScoreMode0(dictscore, false) }
						{1} { this.parseScoreMode1(dictscore, false) };
					//	{2} { this.parseScoreMode2(dictscore) }; // not relevant
					if(agent[1].playstate == true, {
						proxyspace[agent].play;
					});
				});
			}
			{"tuning"}{
				var tuningstart, tuningstr;
				tuningstart = string.find("g");
				tuningstr = string.tr($ , \);
				tuning = tuningstr[tuningstart+1..tuningstr.size-1];
				tuning = tuning.reject({ |c| c.ascii == 10 }); // get rid of char return
				chosenscale.tuning_(tuning.asSymbol);
				scale = chosenscale.semitones.copy.add(12);
			}
			{"tuningpush"}{
				var tuningstart, tuningstr;
				var dictscore, mode;
				tuningstart = string.find("g");
				tuningstr = string.tr($ , \);
				tuning = tuningstr[tuningstart+1..tuningstr.size-1];
				tuning = tuning.reject({ |c| c.ascii == 10 }); // get rid of char return
				chosenscale.tuning_(tuning.asSymbol);
				scale = chosenscale.semitones.copy.add(12);
				agentDict.do({arg agent;
					dictscore = agent[1].scorestring;
					dictscore = dictscore.reject({ |c| c.ascii == 34 }); // get rid of quotation marks
					mode = block{|break|
						["|", "[", "{", ")"].do({arg op, i;						var c = dictscore.find(op);
							if(c.isNil.not, {break.value(i)});
						});
					};
					switch(mode)
						{0} { this.parseScoreMode0(dictscore, false) }
						{1} { this.parseScoreMode1(dictscore, false) };
					//	{2} { this.parseScoreMode2(dictscore) }; // not relevant
					if(agent[1].playstate == true, {
						proxyspace[agent].play;
					});
				});
			}
			{"remind"}{
				this.getMethodsList;
			}
			{"help"}{
				XiiLang.openHelpFile;
			}
			{"tonality"}{
				var doc;
				doc = Document.new;
				doc.name_("scales");
				doc.promptToSave_(false);
				doc.background_(Color.black);
				doc.setStringColor(Color.green, 0, 1000000);
				doc.bounds_(Rect(10, 500, 500, 800));
				doc.font_(Font("Monaco",16));
				doc.string_("Scales: " + ScaleInfo.scales.keys.asArray.sort.asCompileString
				+"\n\n Tunings: "+
				"et12, pythagorean, just, sept1, sept2, mean4, mean5, mean6, kirnberger, werkmeister, vallotti, young, reinhard, wcHarm, wcSJ");
			}
			{"instr"}{
				this.getInstrumentsList;
			}
			{"tonic"}{
				var tonicstart, tstring, tonicstr;
				tonicstart = string.find("c");
				tstring = string.tr($ , \);
				tonicstr = tstring[tonicstart+1..tstring.size-1];
				tonicstr = tonicstr.reject({ |c| c.ascii == 10 }); // get rid of char return
				if(tonicstr.asInteger == 0, { // it's a string
					tonic = 60 + [\C, \CS, \D, \DS, \E, \F, \FS, \G, \GS, \A, \AS, \B].indexOf(tonicstr.toUpper.asSymbol); // midinote 60 is the default
				}, {
					tonic = tonicstr.asInteger;
				});
			}
			{"grid"}{
				var cursorPos, gridstring, meter, grids, gridstart;
				cursorPos = doc.selectionStart; // get cursor pos
				meter = string[string.find(" ")..string.size-1].asInteger;
				if(meter == "grid", {meter = 1});
				gridstring = "";
				50.do({arg i; gridstring = gridstring++if((i%meter)==0, {"|"}, {" "})  });
				doc.string_(doc.string.replace(string, gridstring++"\n"));
			}
			{"kill"}{
				proxyspace.end;
				proxyspace = ProxySpace.new.know_(true);
				snapshotDict[\futures].stop; // ooo not working
				agentDict.do({arg agent;
					agent[2].do({arg routine; routine.stop});
				});
				agentDict.do({arg agent;
					agent[2].stop;
					agent[2] = nil;
				});
			}
			{"group"}{
				var spaces, groupname, op, groupitems;
				// allow for some sloppyness in style
				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string++" "; // add an extra space at the end
				string = string.replace("  ", " ");
				string = string.reject({ |c| c.ascii == 10 }); // get rid of char return
				spaces = string.findAll(" ");
				groupname = string[spaces[0]+1..spaces[1]-1];
				" --->   ixi lang : MAKING A GROUP : ".post; groupname.postln;
				groupname = (this.agentPrefix++groupname).asSymbol; // multidoc support
				op = string.find("->");
				groupitems = [];
				(spaces.size-1).do({arg i;
					if(spaces[i] > op, { groupitems = groupitems.add( string[spaces[i]+1..spaces[i+1]-1].asSymbol ) })
				});
				groups.add(groupname -> groupitems);
			}
			{"sequence"}{
				var spaces, sequenceagent, op, seqagents, typecheck, firsttype, sa, originalstring, originalagents, fullscore;
				var notearr, durarr, sustainarr, instrarr, attackarr, amparr, panarr, score, instrument, quantphase, newInstrFlag = false;
				typecheck = 0;
				notearr = [];
				durarr = [];
				sustainarr = [];
				attackarr = [];
				amparr = [];
				panarr = [];
				instrarr = [];
				score = "";
				originalstring = string;
				" --->   ixi lang : MAKING A SEQUENCE : ".postln;
				// allow for some sloppyness in style
				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string.replace("  ", " ");
				string = string++" "; // add an extra space at the end
				string = string.reject({ |c| c.ascii == 10 }); // get rid of char return
				spaces = string.findAll(" ");
				sequenceagent = string[spaces[0]+1..spaces[1]-1];
				sa = sequenceagent;
				sequenceagent = (this.agentPrefix++sequenceagent).asSymbol; // multidoc support
				op = string.find("->");
				seqagents = [];
				originalagents = [];

				(spaces.size-1).do({arg i;
					if(spaces[i] > op, {
						seqagents = seqagents.add((this.agentPrefix++string[spaces[i]+1..spaces[i+1]-1]).asSymbol);
						originalagents = originalagents.add(string[spaces[i]+1..spaces[i+1]-1]);
					});
				});
				// check if all items are of same type
				firsttype = agentDict[seqagents[0]][1].mode;
				seqagents.do({arg agent; typecheck = typecheck + agentDict[agent][1].mode; });

				// then merge their score into one string (see below the dict idea)
				if((typecheck/seqagents.size) != firsttype, {
					" --->   ixi lang: ERROR! You are trying to mix playmodes".postln;
				}, {
					switch(firsttype)
						{0} {
							seqagents.do({arg agent, i;
								notearr = agentDict[agent][1].notearr;
								durarr = durarr ++ agentDict[agent][1].durarr;
								sustainarr = sustainarr ++ agentDict[agent][1].sustainarr;
								instrarr = instrarr ++ agentDict[agent][1].instrarr;
								attackarr	= attackarr ++ agentDict[agent][1].attackarr;
								panarr	= panarr ++ agentDict[agent][1].panarr;
								score = score ++ agentDict[agent][1].score; // just for creating the score in the doc
							});

							fullscore = (sa++" -> |"++score++"|");
							if(agentDict[sequenceagent].isNil, {
								agentDict[sequenceagent] = [ (), ().add(\amp->0.3), nil];
							}, {
								if(agentDict[sequenceagent][1].scorestring.contains("{"), {newInstrFlag = true }); // free if { instr (Pmono is always on)
							}); // 1st = effectRegistryDict, 2nd = scoreInfoDict, 3rd = placeholder for a routine
								quantphase = agentDict[seqagents[0]][1].quantphase;
								agentDict[sequenceagent][1].scorestring = fullscore.asCompileString;
								agentDict[sequenceagent][1].instrument = "rhythmtrack";
								agentDict[sequenceagent][1].durarr = durarr;
								agentDict[sequenceagent][1].notearr = notearr;
								agentDict[sequenceagent][1].sustainarr = sustainarr;
								agentDict[sequenceagent][1].instrarr = instrarr;
								agentDict[sequenceagent][1].attackarr = attackarr;
								agentDict[sequenceagent][1].panarr = panarr;
								agentDict[sequenceagent][1].score = score;
								agentDict[sequenceagent][1].quantphase = quantphase;
								doc.string_(doc.string.replace(originalstring, fullscore++"\n"));
								this.playScoreMode0(sequenceagent, notearr, durarr, instrarr, sustainarr, attackarr, panarr, quantphase, newInstrFlag, agentDict[sequenceagent][1].morphmode, agentDict[sequenceagent][1].repeats, false);
						}
						{1} {

							seqagents.do({arg agent, i;
								durarr = durarr ++ agentDict[agent][1].durarr;
								sustainarr = sustainarr ++ agentDict[agent][1].sustainarr;
								notearr = notearr ++ agentDict[agent][1].notearr;
								attackarr	= attackarr ++ agentDict[agent][1].attackarr;
								score = score ++ agentDict[agent][1].score; // just for creating the score in the doc
							});

							quantphase = agentDict[seqagents[0]][1].quantphase;
							instrument = agentDict[seqagents[0]][1].instrument;
							fullscore = (sa++" -> "++ instrument ++"["++score++"]");
							if(agentDict[sequenceagent].isNil, {
								agentDict[sequenceagent] = [ (), ().add(\amp->0.3), nil];
							}, {
								if(agentDict[sequenceagent][1].scorestring.contains("{"), {newInstrFlag = true }); // free if { instr (Pmono is always on)
							}); // 1st = effectRegistryDict, 2nd = scoreInfoDict, 3rd = placeholder for a routine

								agentDict[sequenceagent][1].scorestring = fullscore.asCompileString;
								agentDict[sequenceagent][1].instrument = instrument;
								agentDict[sequenceagent][1].durarr = durarr;
								agentDict[sequenceagent][1].sustainarr = sustainarr;
								agentDict[sequenceagent][1].notearr = notearr;
								agentDict[sequenceagent][1].attackarr = attackarr;
								agentDict[sequenceagent][1].panarr = panarr;
								agentDict[sequenceagent][1].score = score;
								agentDict[sequenceagent][1].quantphase = quantphase;

								doc.string_(doc.string.replace(originalstring, fullscore++"\n"));
								this.playScoreMode1(sequenceagent, notearr, durarr, sustainarr, attackarr, panarr, instrument, quantphase, newInstrFlag, agentDict[sequenceagent][1].repeats, false);
						}
						{2} {
							seqagents.do({arg agent, i;
								durarr = durarr ++ agentDict[agent][1].durarr;
								amparr = notearr ++ agentDict[agent][1].amparr;
								attackarr	= attackarr ++ agentDict[agent][1].attackarr;
								score = score ++ agentDict[agent][1].score; // just for creating the score in the doc
							});
							quantphase = agentDict[seqagents[0]][1].quantphase;
							instrument = agentDict[seqagents[0]][1].instrument;
							fullscore = (sa++" -> "++ instrument ++"{"++score++"}");
							if(agentDict[sequenceagent].isNil, {
								agentDict[sequenceagent] = [ (), ().add(\amp->0.3), nil];
							}, {
								if(agentDict[sequenceagent][1].scorestring.contains("{"), {newInstrFlag = true }); // free if { instr (Pmono is always on)
							}); // 1st = effectRegistryDict, 2nd = scoreInfoDict, 3rd = placeholder for a routine
								agentDict[sequenceagent][1].scorestring = fullscore.asCompileString;
								agentDict[sequenceagent][1].instrument = instrument;
								agentDict[sequenceagent][1].durarr = durarr;
								agentDict[sequenceagent][1].amparr = amparr;
								agentDict[sequenceagent][1].panarr = panarr;
								agentDict[sequenceagent][1].score = score;
								agentDict[sequenceagent][1].quantphase = quantphase;
								doc.string_(doc.string.replace(originalstring, fullscore++"\n"));
								this.playScoreMode2(sequenceagent, amparr, durarr, panarr, instrument, quantphase, newInstrFlag, agentDict[sequenceagent][1].repeats, false);
						};
				});
			}

			// methods (called verbs in ixi lang) dealt with in the parsMethod below (these change string in doc)
			{"doze"} {
				this.parseMethod(string);
			}
			{"perk"}{
				this.parseMethod(string);
			}
			{"nap"}{
				this.parseMethod(string);
			}
			{"shake"}{
				this.parseMethod(string);
			}
			{"swap"}{
				this.parseMethod(string);
			}
			{"replace"}{
				this.parseMethod(string);
			}
			{"insert"}{
				this.parseMethod(string);
			}
			{"remove"}{
				this.parseMethod(string);
			}
			{">shift"}{
				this.parseMethod(string);
			}
			{"<shift"}{
				this.parseMethod(string);
			}
			{"invert"}{
				this.parseMethod(string);
			}
			{"expand"}{
				this.parseMethod(string);
			}
			{"revert"}{
				this.parseMethod(string);
			}
			{"up"}{
				this.parseMethod(string);
			}
			{"down"}{
				this.parseMethod(string);
			}
			{"yoyo"}{
				this.parseMethod(string);
			}
			{"order"}{
				this.parseMethod(string);
			}
			{"hash"}{
				this.parseMethod(string);
			}
			{"beer"}{
				this.parseMethod(string);
			}
			{"coffee"}{
				this.parseMethod(string);
			}
			{"LSD"}{
				this.parseMethod(string);
			}
			{"detox"}{
				this.parseMethod(string);
			}
			// scheme-like verbs
			{"+"}{
				this.parseMethod(string);
			}
			{"-"}{
				this.parseMethod(string);
			}
			{"*"}{
				this.parseMethod(string);
			}
			{"/"}{
				this.parseMethod(string);
			}
			{"!"}{
				this.parseMethod(string);
			}
			{"("}{
				this.parseMethod(string);
			}
			{"<"}{
				this.parseMethod(string);
			}
			{"^"}{
				this.parseMethod(string);
			}
			{"@"}{
				this.parseMethod(string, return);
			}
			{"suicide"}{
				var chance, time, op;
				var nrstart = string.find("e")+1;
				if(string.contains(":"), {
					string = string.tr($ , \);
					op = string.find(":");
					chance = string[nrstart..op].asInteger;
					time = string[op+1..string.size-1].asInteger;
					suicidefork = fork{
						inf.do({
						["---  WILL I DIE NOW? ---", "---  HAS IT COME TO AN END?  ---", "---  WHEN WILL I DIE?  ---", "---  MORRISSEY  ---", "---  ACTUALLY, WAIT A MINUTE...  ---", "---  I'VE HAD IT  ---", "---  THIS IS THE END  ---", "---  I'M PATHETIC  ---"].choose.postln;
						if((chance/100).coin, {
							this.opInterpreter("savescore" + ("suicide@"++Date.getDate.stamp.asString)); // save score first
							2.wait;
							0.exit; // kill supercollider
						});
						time.wait;
						});
					};
				});
			}
			{"hotline"}{
				"---  I'M ALL RIGHT NOW, THANKS! ---".postln;
				suicidefork.stop;
			}
			{"midiclients"}{
				"-----------   Avalable midiclients   ------------ ".postln;
				MIDIClient.destinations.do({ |x, i| Post << "device " << i << Char.tab << x << Char.nl });
			}
			{"midiout"}{
				var nrstart, destination;
				"-----------   MIDI OUT    ------------ ".postln;
				nrstart = string.find("t")+1;
				destination = string[nrstart..string.size-1].asInteger;
				midiclient = MIDIOut(destination);
				"---> You set the MIDI out to : ".post; MIDIClient.destinations[destination].postln;
				MIDIClockOut(destination).play; // XXX experimental
			}
			{"matrix"}{
				var spaces, size=8, direction=\x;
				spaces = string.findAll(" ");
				if(spaces.size > 0, { size = string[spaces[0]..spaces[1]].asInteger; });
				if(spaces.size > 1, {
					direction = string[spaces[1]+1..string.size-1].tr($ , \).asSymbol;
				});
				matrixArray = matrixArray.add( XiiLangMatrix.new(size, direction, instrDict, doccolor, oncolor) );
			}
			{"coder"}{
				var xiilang, tempstring, scorestring, coderarraysum, quantspaces, quant;

				xiilang = XiiLang(initargs[0], initargs[1], initargs[2], true, initargs[4], numChannels: numChan);
				xiilang.doc.name_("ixi lang coder");
				xiilang.doc.keyDownAction_({| thisdoc, char, mod, unicode, keycode |
					var linenr, string;
				//TODO if((mod & 524288 == 524288) && ((keycode==124)||(keycode==123)||(keycode==125)||(keycode==126)), {
				// 	linenr = thisdoc.string[..thisdoc.selectionStart-1].split($\n).size;
				// 	thisdoc.selectLine(linenr);
				// 	string = thisdoc.selectedString;
				// 	if(keycode==123, { // not 124, 125,
				// 		xiilang.freeAgent(string);
				// 		}, {
				// 			xiilang.opInterpreter(string);
				// 	});
				// });
					// here adding what the coder is about
					if(char.isAlpha, {
						Synth(instrDict[char.asSymbol], [\freq, 60.midicps]); // original
					});
				});
			}
			{"twitter"}{
				var xiilang, tweetDict, getTweets, currentLine, newLines, routine;
				xiilang = XiiLang(initargs[0], initargs[1], initargs[2], true, initargs[4]);
				xiilang.doc.name_("ixi lang twitter client");
				xiilang.doc.string_("// this window evaluates your #ixilang twitter messages");
				tweetDict = ();
				currentLine = 1; // one empty line at the top
				newLines = 0;

				// TODO: unixCmd to start moai main.lua

				getTweets = {
					var file, string, allLineBreaks, newdocstring;
					file = File("/Users/thor/quaziir/mobilecoding/moai-sdk/thor/ixiTwitter/ixitweets","r");
					string = file.readAllString;
					file.close;
					string = string.replace("#ixilang", "");
					//string = string.replace("   ","");
					allLineBreaks = string.findAll("\n");
					newdocstring = xiilang.doc.string;
					newLines = 0;
					allLineBreaks.do({arg thisLineBreak, i;
						var prevLineBreak, line, id, msg;
						prevLineBreak = allLineBreaks[i-1];
						if(prevLineBreak.isNil, { prevLineBreak = 0 });
						line = string[prevLineBreak..thisLineBreak];
						id = line[1..line.findAll(",")[0]-1];
						if(tweetDict[id.asSymbol].isNil, {
							msg = line[line.findAll(",")[0]+3..line.findAll(",")[1]-1];
							tweetDict[id.asSymbol] = msg;
							newdocstring = newdocstring ++ "\n" ++ msg;
							newLines = newLines + 1;
						});
					});
					xiilang.doc.string = newdocstring;
					{
						newLines.do({arg i;
							var xiistring;
							"running newlines".postln;
							currentLine = currentLine + 1;
							xiilang.doc.selectLine(currentLine);
							xiistring = xiilang.doc.selectedString;
							try{this.opInterpreter(xiistring)};

							0.5.wait;
						});
					}.fork(AppClock);
					//Post << tweetDict;
				};

				routine = {{
					getTweets.value;
					5.wait;
				}.loop}.fork(AppClock);


			}
			{"autocode"}{
				var agent, mode, instrument, score, line, charloc, cursorPos, density, lines, spaces, nextline;

				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string.replace("  ", " ");
				string = string++" "; // add a space in order to find end of agent (if no argument)
//				[\firstspace, string.findAll(" ")[0]].postln;
//				[\endspace, string.findAll(" ")[1]].postln;
//				charloc = doc.selectedRangeLocation;
				spaces = doc.string.findAll(" ");
			//	nextline = if(spaces.indexOfGreaterThan(charloc).isNil, {
//						charloc;
//					},{
//						spaces[spaces.indexOfGreaterThan(charloc)];
//					});
//				charloc = nextline;
//
			//	charloc = spaces[spaces.indexOfGreaterThan(charloc)-1];
				charloc = doc.selectionStart;

				//charloc = string.findAll(" ")[1]+1;
				{doc.setString("\n", charloc, 0)}.defer;
				lines = "";
				string.collect({arg char; if(char.isDecDigit, { lines = lines++char }) });
				lines = lines.asInteger;
				{
					lines.do({
						mode = [0,1,2].wchoose([0.5, 0.35, 0.15]);
						agent = "abcdefghijklmnopqrstuvxyzaeyiuxz".scramble[0..(2+(4.rand))];
						density = rrand(0.15, 0.45);
						switch(mode)
							{0}{
								score = "";
								[16, 20, 24].choose.do({arg char;
									score = score ++ if(density.coin, {
										"abcdefghijklmnopqrstuvxABCDEFGHIJKLMNOPQRSTUVXYZ".scramble.choose;
									}, {" "});
								});
								score = "|"++score++"|";
								line = agent+"->"+score+"\n";
							}
							{1}{
								instrument = (ixiInstr.returnMelodicInstr++
								ixiInstr.returnMelodicInstr++ixiInstr.returnMelodicInstr++
								ixiInstr.returnMelodicInstr++ixiInstr.returnPercussiveInstr).choose;
								score = "";
								[16, 20, 24].choose.do({
									score = score ++ if(density.coin, {10.rand}, {" "});
								});
								score = instrument++"["++score++"]";
								line = agent+"->"+score+"\n";
							}
							{2}{
								instrument = ixiInstr.returnPercussiveInstr.choose;
								score = "";
								[16, 20, 24].choose.do({
									score = score ++ if(density.coin, {10.rand}, {" "});
								});
								score = instrument++"{"++score++"}";
								line = agent+"->"+score+"\n";
							};
						line.do({arg char, i;
							charloc = charloc + 1;
							{doc.setString(char.asString, charloc, 0)}.defer;
							[0.1, 0.05, 0.5].wchoose([0.5, 0.4, 0.1]).wait;
						});
						1.wait;
					{this.opInterpreter(line)}.defer;
					});
				}.fork(TempoClock.new);
			}
			{"new"}{
				XiiLangGUI.new(projectname, numChannels: numChan);
			}
			{"gui"}{
				XiiLangGUI.new(projectname, numChannels: numChan);
			}
			{"savescore"}{
				var sessionstart, sessionend, session;
				var offsettime;

				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string.replace("  ", " ");
				string = string++" "; // add a space in order to find end of agent (if no argument)
				sessionstart = string.findAll(" ")[0];
				sessionend = string.findAll(" ")[1];
				session = string[sessionstart+1..sessionend-1];
				offsettime = scoreArray[0][0];
				scoreArray = scoreArray.collect({arg event; [event[0]-offsettime, event[1]]});
				[randomseed, scoreArray.copy].writeArchive(this.projectDirname++"/scores/"++session++".scr");
			}
			{"playscore"}{
				var sessionstart, sessionend, session, variation, varstart, varend, score, offsettime;
				variation = nil;
				string = string.replace("    ", " ");
				string = string.replace("   ", " ");
				string = string.replace("  ", " ");
				string = string++" "; // add a space in order to find end of agent (if no argument)
				sessionstart = string.findAll(" ")[0];
				sessionend = string.findAll(" ")[1];
				session = string[sessionstart+1..sessionend-1];
				if(string.findAll(" ")[2].isNil.not, {
					varstart = string.findAll(" ")[1];
					varend = string.findAll(" ")[2];
					variation = string[varstart+1..varend-1];
				});
				//doc.onClose; // call this to end all agents and groups in this particular doc if it has been running agents
				#randomseed, score = Object.readArchive(this.projectDirname++"/scores/"++session++".scr");
				if(variation.isNil.not, { randomseed = variation.ascii.pyramid.pyramid.sum; "playing variation: %".postf(variation) }); // overwrite the orig seed to get a variation
				XiiLang.new( projectname, key, true, false, language, nil, [randomseed, score]);
			}
			{"newrec"}{
				" --->   ixi lang: clearing score array".postln;
				scoreArray = [];
				scoreArray = scoreArray.add([Main.elapsedTime, string]); // recording the performance
			}
			;
		}

	playScore{ arg score;
		var offsettime;
		offsettime = score[0][0];
		doc.string = "";
		win.onClose;
		{
		//	thisThread.randSeed = randomseed;
			score.do({ arg event;
				(event[0]-offsettime).wait;
				//(1).wait;
				if(	event[1].contains("future").not &&
					event[1].contains("snapshot").not &&
					event[1].contains("suicide").not &&
					event[1].contains("savescore").not, {
					doc.string_(doc.string++"\n"++event[1]);
					this.opInterpreter(event[1]);
				});
				offsettime = event[0];
			});
		}.fork(AppClock);
	}
	// method invoked on alt+left arrow, for easy freeing of an agent (line)
	// XXX FIXING THIS
	freeAgent { arg string;
		var prestring, splitloc, agent, linenr, stringstart, stringend, pureagentname;
		var recursionfunc;
		linenr = doc.string[..doc.selectionStart-1].split($\n).size;
		//doc.selectLine(linenr);
		string = string.tr($ , \);
		splitloc = string.find("->");
		pureagentname = string[0..splitloc-1]; // get the name of the agent
		agent = (this.agentPrefix++pureagentname).asSymbol;
		[\pureagentnameX, pureagentname].postln;

		#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);

		[pureagentname, stringstart, stringend].postln;

		recursionfunc = {arg subagent;
				if(groups[subagent].isNil.not, {
			//		"---- The subagent is a group. Name : ".post; subagent.postln;
					groups[subagent].do({arg agent;
			//			"GROUP AGENT recursing : ".post; agent.postln;
				 		recursionfunc.value((this.agentPrefix++agent).asSymbol); // recursion
					});
				}, {
			//		"FREEING AGENT : ".post; subagent.postln;
					proxyspace[subagent.asSymbol].clear;
					agentDict[subagent.asSymbol][1].playstate = false;
				});
		};

		if(metaAgentDict[agent].isNil.not, {
			// "This is the agent : ".post; agent.post; " ... and here are the subagents:".post; metaAgentDict[agent].agents.postln;

			metaAgentDict[agent].agents.do({arg subagent;
				if(groups[subagent].isNil.not, {
					recursionfunc.value(subagent);
				}, {
				//	"FREEING AGENT __ : ".post; subagent.postln;
					proxyspace[subagent].clear;
					agentDict[subagent][1].playstate = false;
				});
			});
		});

		proxyspace[agent].clear;
		agentDict[agent] = nil;
		metaAgentDict[agent] = nil;
	//\deb.postln;
		{doc.setStringColor(deadcolor, stringstart, stringend-stringstart)}.defer;
	//	{doc.setStringColor(deadcolor, doc.selectionStart, doc.selectionSize)}.defer(0.1); // killed code is red
	}

	interpret { arg cmd;
		// used for debugging from ixi lang doc
		cmd.interpret;
	}

	parseSnapshot{ arg string;
		var snapshotname, splitloc, agentDICT;

		string = string.replace("    ", " ");
		string = string.replace("   ", " ");
		string = string.replace("  ", " ");
		string = string.reject({ |c| c.ascii == 10 }); // get rid of char return

		if(string.contains("->"), { // STORE A SNAPSHOT
			" --->   ixi lang: storing a snapshot".postln;
			string = string.tr($ , \);
			splitloc = string.find(">");
			snapshotname = string[splitloc+1 .. string.size-1];
			snapshotDict[snapshotname.asSymbol] = nil; // remove properly;
			snapshotDict[snapshotname.asSymbol] = agentDict.deepCopy;
		}, {	// RECALL A SNAPSHOT
			splitloc = string.find(" ");
			snapshotname = string[splitloc+1 .. string.size-1];
			agentDICT = snapshotDict[snapshotname.asSymbol]; // a local variable of a snapshot agent dictionary (not the global one)

			if(agentDICT.isNil, {
				" --->   ixi lang: there is no snapshot with that name!".postln;
			}, {
				" --->   ixi lang: recalling a snapshot".postln;
				// stop all agents that are not in the snapshot
				agentDict.keys.do({arg agent;
					var allreturns, stringstart, stringend, pureagentname;

					pureagentname = agent.asString;
					pureagentname = pureagentname[1..pureagentname.size-1];

					if(agentDICT[agent].isNil, {
						agentDict[agent][1].playstate = false;
						Pdef(agent).stop; // new
						proxyspace[agent].stop;
						// proxyspace[agent].objects[0].array[0].mute;
								allreturns = doc.string.findAll("\n") ++ doc.string.findAll(""++13.asAscii);
						// the following checks if it's exactly the same agent name (and not confusing joe and joel)
						#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);
								(stringend.notNil && stringstart.notNil).if({
						doc.setStringColor(offcolor, stringstart, stringend-stringstart);
								});
					});
				});
				// then run the agents in the snapshot and replace strings in doc
				agentDICT.keysValuesDo({arg keyagentname, agentDictItem;
					var allreturns, stringstart, stringend, thisline, agentname, pureagentname, dictscore, mode, cursorPos;
					agentname = keyagentname.asString;
					pureagentname = agentname[1..agentname.size-1];

					// --  0)  Check if the agent is playing or not in that snapshot (and not play agents which have ended)
					if((agentDictItem[1].playstate == true) , {
						// --  1)  Find the agent in the doc
						allreturns = doc.string.findAll("\n") ++ doc.string.findAll(""++13.asAscii);
						// the following checks if it's exactly the same agent name (and not confusing joe and joel)
						#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);
						thisline = doc.string[stringstart..stringend];
						dictscore = agentDictItem[1].scorestring;
						dictscore = dictscore.reject({ |c| c.ascii == 34 }); // get rid of quotation marks

						// --  2)  Swap the string in the doc
						// either with the simple swap
								(stringend.notNil && stringstart.notNil).if({
						doc.string_( dictscore, stringstart, stringend-stringstart); // this one keeps text colour
						doc.setStringColor(oncolor, stringstart, stringend-stringstart);
								});
						// --  3)  Run the code (parse it) - IF playstate is true (in case it's been dozed)
						try{ // try, because if loading from "load", then there will be no proxyspace yet
							if(proxyspace[keyagentname].objects[0].array[0].muteCount == 1, {
							proxyspace[keyagentname].objects[0].array[0].unmute;
						});
						};

						mode = block{|break|
							["|", "[", "{", "~", ")"].do({arg op, i;									var c = dictscore.find(op);
								if(c.isNil.not, {break.value(i)});
							});
						};

						switch(mode)
							{0} { proxyspace[keyagentname].play; this.parseScoreMode0(dictscore, false, true) }
							{1} { proxyspace[keyagentname].play; this.parseScoreMode1(dictscore, false, true) }
							{2} { proxyspace[keyagentname].play; this.parseScoreMode2(dictscore, false, true) }
							{3} { proxyspace[keyagentname].play; this.opInterpreter( dictscore, false, true ) };
						//proxyspace[keyagentname].play;
						scoreArray = scoreArray.add([Main.elapsedTime, dictscore]);

						// --  4)  Set the effects that were active when the snapshot was taken

						10.do({arg i; proxyspace[keyagentname][i+1] =  nil }); // remove all effects (10 max) (+1, as 0 is Pdef)

						agentDICT[keyagentname][0].keys.do({arg key, i;
							proxyspace[keyagentname][i+1] = \filter -> effectDict[key.asSymbol];
							scoreArray = scoreArray.add([Main.elapsedTime, pureagentname + ">>" + key]);
						});

					}, { // agent is not playing in the snapshot
						Pdef(keyagentname).stop; // new
						proxyspace[keyagentname].stop;
						#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);
									(stringend.notNil && stringstart.notNil).if({
						doc.setStringColor(offcolor, stringstart, stringend-stringstart);
									})
					});
				});
			});
		});
	}

	parsePostfixArgs {arg postfixstring;
		var sustainstartloc, sustainendloc, sustainstring, sustainarr;
		var attacksymbols, attackstartloc, attackendloc, attackstring, attackarr;
		var pansymbol, panstartloc, panendloc, panstring, panarr;
		var argDict, multiplication, multloc;
		multiplication = 1;

		argDict = ();
		argDict[\timestretch] = 1;
		argDict[\transposition] = 0;
		argDict[\silences] = 0;
		argDict[\repeats] = inf;

		postfixstring.do({arg char, i;
			var timestretch, transposition, silences, repeats;
			if( (char==$*) || (char==$/), {
				timestretch = "";
				block{|break|
					postfixstring[i+1..postfixstring.size-1].do({arg item;
						if((item.isAlphaNum) || (item == $.), {timestretch = timestretch ++ item }, {break.value});
					});
				};
				timestretch = timestretch.asFloat;
				if( (char==$/), {timestretch = timestretch.reciprocal; });
				argDict[\timestretch] = timestretch.asFloat;
			});
			if( (char==$+) || (char==$-), {
				transposition = "";
				block{|break|
					postfixstring[i+1..postfixstring.size-1].do({arg item;
						if(item.isAlphaNum || (item == $.), {transposition = transposition ++ item }, {break.value});
					});
				};
				transposition = transposition.asFloat;
				if( (char==$-), {transposition = transposition.neg; });
				argDict[\transposition] = transposition;
			});
			if( char==$!, {
				silences = "";
				block{|break|
					postfixstring[i+1..postfixstring.size-1].do({arg item;
						if(item.isAlphaNum, {silences = silences ++ item }, {break.value});
					});
				};
				argDict[\silences] = silences.asInteger;
			});
			if( char==$@, {
				repeats = "";
				block{|break|
					postfixstring[i+1..postfixstring.size-1].do({arg item;
						if(item.isAlphaNum, {repeats = repeats ++ item }, {break.value});
					});
				};
				argDict[\repeats] = repeats.asInteger;
			});
		});

		// -- sustain --
		sustainstartloc = postfixstring.find("(");
		sustainendloc = postfixstring.find(")");
		if(sustainstartloc.isNil, {
			sustainstring = "4";
		}, {
			sustainstring = postfixstring[sustainstartloc+1..sustainendloc-1];
			if(sustainstring.contains("_"), {
				multloc = postfixstring.find("_");
				sustainstring = postfixstring[sustainstartloc+1..multloc-1];
				multiplication = postfixstring[multloc+1..sustainendloc-1].asFloat;
			})
		});
		sustainstring.do({arg dur; var durint;
			durint = dur.asString.asInteger;
			if(durint == 0, {durint = 1}); // make sure durint is not 0 - which will crash the language
			sustainarr = sustainarr.add(durint.reciprocal);
		});

		sustainarr = sustainarr*multiplication;
		argDict.add(\sustainarr -> sustainarr);

		// -- attack --
		attacksymbols = postfixstring.findAll("^");
		if(attacksymbols.isNil, {
			attackstring = "5";
		}, {
			attackstartloc = attacksymbols[0];
			attackendloc = attacksymbols[1];
			attackstring = postfixstring[attackstartloc+1..attackendloc-1];
		});
		attackstring.do({arg att;
			attackarr = attackarr.add(att.asString.asInteger/9); // values range from 0 to 1.0
		 });
		argDict.add(\attackarr -> attackarr);

		// -- panning --
		pansymbol = postfixstring.find("<");
		if(pansymbol.isNil, {
			panstring = "5";
		}, {
			panstartloc = postfixstring.find("<");
			panendloc = postfixstring.find(">");
			panstring = postfixstring[panstartloc+1..panendloc-1];
		});
		panstring.do({arg pan;
			if(numChan == 2, {
				panarr = panarr.add(pan.asString.asInteger.linlin(1, 9, -1, 1)); // 1 to 9 are mapped to panning of -1.0 to 1.0 (Pan2)
			}, {
				panarr = panarr.add(pan.asString.asInteger.linlin(1, 9, 0, 2 - (2/numChan))); // 1 to 9 are mapped to panning of 0 - (2 - (2/numChan)) (PanAz)
			});
		 });
		argDict.add(\panarr -> panarr);
		^argDict;
	}

	// PERCUSSIVE MODE
	parseScoreMode0 {arg string, return, snapshot=false;
		var agent, pureagent, score, splitloc, endchar, agentstring, silenceicon, silences, scorestring, timestretch=1, postfixargs;
		var durarr, notearr, sustainarr, spacecount, instrarr, instrstring, quantphase, empty, outbus;
		var attacksymbols, attackstartloc, attackendloc, attackstring, attackarr, panarr, transposition, repeats;
		var startWempty = false;
		var newInstrFlag = false;
		var postfixArgDict, stringstart, stringend;
		var prestring, scorestartloc, morphmode;

		scorestring = string.reject({arg char; char.ascii == 10 }); // to store in agentDict
		scorestartloc = string.find("|");
		prestring = string[0..scorestartloc-1].tr($ , \); // get rid of spaces until score
		splitloc = prestring.find("->");
		agent = prestring[0..splitloc-1]; // get the name of the agent
		pureagent = agent;
		#stringstart, stringend = this.findStringStartEnd(doc, pureagent);

		morphmode = prestring[splitloc+2..prestring.size];
		if(morphmode.size < 1, { morphmode = nil });

		score = string[scorestartloc+1..string.size-1];
		endchar = score.find("|"); // the index (int) of the end op in the string

		// -- parse the postfix args (after the score)
		postfixargs = score[endchar+1..score.size-1].tr($ , \); // allowing for spaces
		postfixArgDict = this.parsePostfixArgs(postfixargs);
		sustainarr = postfixArgDict.sustainarr;
		attackarr = postfixArgDict.attackarr;
		timestretch = postfixArgDict.timestretch;
		silences = postfixArgDict.silences;
		panarr = postfixArgDict.panarr;
		transposition = postfixArgDict.transposition;
		repeats = postfixArgDict.repeats;
		notearr = [60+transposition];

		score = score[0..endchar-1]; // get rid of the function marker
		agent = (this.agentPrefix++agent).asSymbol;

		// -- create a new agent if needed
		if(agentDict[agent].isNil, {
			agentDict[agent] = [(), ().add(\amp->0.5), []];
		}, {
			if(agentDict[agent][1].scorestring.contains("{"), { newInstrFlag = true });
			// trick to free if the agent was { instr (Pmono is always on)
		}); // 1st = effectRegistryDict, 2nd = scoreInfoDict, 3rd = placeholder for a routine

		// ------------- the instrument -------------------
		instrstring = score.tr($ , \).tr($., \);
		instrarr = [];
		instrstring.collect({arg instr, i; instrarr = instrarr.add(instrDict[instr.asSymbol]) });

		// -------------    the score   -------------------
		quantphase=0;
		spacecount = 0;
		durarr = [];
		score.do({arg char, i;
			if(((char==$ ) || (char==$.)) && (i==0), { startWempty = true; });
			if(((char==$ ) || (char==$.)), {
				spacecount = spacecount+1;
			}, {
				if(i != 0, { // not adding the the first instr
					durarr = durarr.add(spacecount+1); // adding to dur array + include the instr char
					spacecount = 0;
				});
			});
			if(i==(score.size-1), { // fixing at the end of the loop
				durarr = durarr.add(spacecount+1);
				if(startWempty, {
					quantphase = (durarr[0]-1)/4;
					empty = durarr.removeAt(0)-1; // minus the added instr (since we're only interested in spaces)
					durarr[durarr.size-1] = durarr.last+empty; // adding the last space(s) to the first space(s)
				});
			});
		});

		durarr[durarr.size-1] = durarr.last+silences; // adding silences to the end of the score
		durarr = durarr/4;
		durarr = durarr*timestretch; // duration is stretched by timestretch var

		agentDict[agent][1].mode = 0;
		agentDict[agent][1].morphmode = morphmode;
		agentDict[agent][1].quantphase = quantphase;
		agentDict[agent][1].durarr = durarr;
		agentDict[agent][1].notearr = notearr; // will only store the transposition
		agentDict[agent][1].instrarr = instrarr;
		agentDict[agent][1].sustainarr = sustainarr;
		agentDict[agent][1].attackarr = attackarr;
		agentDict[agent][1].panarr = panarr;
		agentDict[agent][1].repeats = repeats;
		agentDict[agent][1].score = score;
		agentDict[agent][1].scorestring = scorestring.asCompileString;
		agentDict[agent][1].instrument = "rhythmtrack";

		{doc.setStringColor(oncolor, stringstart, stringend-stringstart)}.defer;
//		{doc.setStringColor(oncolor, doc.selectionStart, doc.selectionSize)}.defer(0.1);  // if code is green (sleeping)
		"------    ixi lang: Created Percussive Agent : ".post; pureagent.postln; agentDict[agent].postln;
		^this.playScoreMode0(agent, notearr, durarr, instrarr, sustainarr, attackarr, panarr, quantphase, newInstrFlag, morphmode, repeats, return, snapshot);
	}

	// MELODIC MODE, rjk
	parseScoreMode1 {arg string, return, snapshot=false;
		var agent, pureagent, score, scorestartloc, splitloc, endchar, agentstring, instrument, instrstring, timestretch=1, transposition=0;
		var prestring, silenceicon, silences, postfixargs, newInstrFlag = false;
		var durarr, sustainarr, spacecount, notearr, notestring, quantphase, empty, outbus, repeats;
		var sustainstartloc, sustainendloc, sustainstring;
		var attacksymbols, attackstartloc, attackendloc, attackstring, attackarr, panarr;
		var startWempty = false;
		var channelicon, midichannel;
		var postfixArgDict, stringstart, stringend;

		channelicon = 0;
		scorestartloc = string.find("[");
		prestring = string[0..scorestartloc-1].tr($ , \); // get rid of spaces until score
		splitloc = prestring.find("->");
		if(splitloc.isNil, { // no assignment operator
			// agent = prestring[0..prestring.size]; // get the name of the agent RJK bug!
			agent = prestring; // get the name of the agent
			instrument = agent;
		}, {
			agent = prestring[0..splitloc-1]; // get the name of the agent
			instrument = prestring[splitloc+2..prestring.size - 1];
		});
		pureagent = agent;
		agent = (this.agentPrefix++pureagent).asSymbol;  //rjk

		#stringstart, stringend = this.findStringStartEnd(doc, pureagent);

		score = string[scorestartloc+1..string.size-1];
		endchar = score.find("]"); // the index (int) of the end op in the string

		// -- parse the postfix args (after the score)
		postfixargs = score[endchar+1..score.size-1].tr($ , \); // allowing for spaces
		postfixArgDict = this.parsePostfixArgs(postfixargs);
		sustainarr = postfixArgDict.sustainarr;
		attackarr = postfixArgDict.attackarr;
		timestretch = postfixArgDict.timestretch;
		silences = postfixArgDict.silences;
		transposition = postfixArgDict.transposition;
		repeats = postfixArgDict.repeats;

		panarr = postfixArgDict.panarr;

		channelicon = score.find("c");
		midichannel = if(channelicon.isNil.not, { score[channelicon+1..channelicon+3].asInteger - 1 }, { 0 });

		score = score[0..endchar-1]; // get rid of the function marker

		if(agentDict[agent].isNil, {
			agentDict[agent] = [(), ().add(\amp->0.5), []];
		}, {
			if(agentDict[agent][1].scorestring.contains("{"), { newInstrFlag = true }); // trick to free if the agent was { instr (Pmono is always on)
		}); // 1st = effectRegistryDict, 2nd = scoreInfoDict, 3rd = placeholder for a routine

		// ------------- the notes -------------------
		notestring = score.tr($ , \).($., \);
		notearr = [];
		notestring.collect({arg note, i;
			var scalenote, thisnote, chord;
			thisnote = note.asString.asInteger; // if thisnote is 0 then it's a chord (since "a".asInteger becomes 0)
			if(thisnote == 0, { // if it is a CHORD
				chord = varDict[note.asSymbol];
				if(chord.isNil, { // if using a chord (item in dict) that does not exist (say x) - to prevent error posting
					if(note != $. , {notearr = notearr.add('\s') });
				}, {
					notearr = notearr.add(chord);
				});
			}, { // it's not a chord but a NOTE
				scalenote = scale[thisnote-1];  // start with 1 as fundamental
				if(scalenote.isNil, {scalenote = scale[(thisnote%scale.size)]+12}); // wrap the scale but add octave
				notearr = notearr.add(scalenote);
			});
		});
		// adding 59 to start with C (and user inputs are 1 as C, 3 as E, 5 as G, etc.)
		notearr = notearr + tonic + transposition; // if added after the score array

		// -------------    the score   -------------------
		quantphase=0;
		spacecount = 0;
		durarr = [];
		score.do({arg char, i;
			if(((char==$ ) || (char==$.)) && (i==0), { startWempty = true; });
			if(((char==$ ) || (char==$.)), {
				spacecount = spacecount+1;
			}, {
				if(i != 0, { // not adding the the first instr
					durarr = durarr.add(spacecount+1); // adding to dur array + include the instr char
					spacecount = 0;
				});
			});
			if(i==(score.size-1), { // fixing at the end of the loop
				durarr = durarr.add(spacecount+1);
				if(startWempty, {
					quantphase = (durarr[0]-1)/4;
					empty = durarr.removeAt(0)-1; // minus the added instr (since we're only interested in spaces)
					durarr[durarr.size-1] = durarr.last+empty; // adding the last space(s) to the first space(s)
				});
			});
		});
		durarr[durarr.size-1] = durarr.last+silences; // adding silences to the end of the score
		durarr = durarr/4;
		durarr = durarr*timestretch; // duration is stretched by timestretch var


		agentDict[agent][1].mode = 1;
		agentDict[agent][1].quantphase = quantphase;
		agentDict[agent][1].durarr = durarr;
		agentDict[agent][1].notearr = notearr;
		agentDict[agent][1].sustainarr = sustainarr;
		agentDict[agent][1].attackarr = attackarr;
		agentDict[agent][1].panarr = panarr;
		agentDict[agent][1].score = score;
		agentDict[agent][1].repeats = repeats;
		agentDict[agent][1].scorestring = string.asCompileString;
		agentDict[agent][1].instrument = instrument;
		agentDict[agent][1].midichannel = midichannel;

		{doc.setStringColor(oncolor, stringstart, stringend-stringstart)}.defer;
		//{doc.setStringColor(oncolor, doc.selectionStart, doc.selectionSize)}.defer(0.1);  // if code is green (sleeping)
		"------    ixi lang: Created Melodic Agent : ".post; pureagent.postln; agentDict[agent].postln;

		^this.playScoreMode1(agent, notearr, durarr, sustainarr, attackarr, panarr, instrument, quantphase, newInstrFlag, midichannel, repeats, return, snapshot);
		// this has to be below the playscore method
	}

	// CONCRETE MODE
	parseScoreMode2 {arg string, return, snapshot=false;
		var agent, pureagent, score, scorestartloc, splitloc, endchar, agentstring, instrument, instrstring, timestretch=1;
		var prestring, silenceicon, silences, postfixargs, panarr, newInstrFlag;
		var durarr, pitch, spacecount, amparr, ampstring, quantphase, empty, outbus, repeats;
		var startWempty = false;
		var postfixArgDict, stringstart, stringend;

		scorestartloc = string.find("{");
		prestring = string[0..scorestartloc-1].tr($ , \); // get rid of spaces until score
		splitloc = prestring.find("->");
		if(splitloc.isNil, { // no assignment operator
			agent = prestring[0..prestring.size]; // get the name of the agent
			instrument = agent;
		}, {
			agent = prestring[0..splitloc-1]; // get the name of the agent
			instrument = prestring[splitloc+2..prestring.size-1];
		});
		pureagent = agent;
		#stringstart, stringend = this.findStringStartEnd(doc, pureagent);
		agent = (this.agentPrefix++agent).asSymbol;
		score = string[scorestartloc+1..string.size-1];
		endchar = score.find("}"); // the index (int) of the end op in the string

		// -- parse the postfix args (after the score)
		postfixargs = score[endchar+1..score.size-1].tr($ , \); // allowing for spaces
		postfixArgDict = this.parsePostfixArgs(postfixargs);
		timestretch = postfixArgDict.timestretch;
		silences = postfixArgDict.silences;
		panarr = postfixArgDict.panarr;
		pitch = 60 + postfixArgDict.transposition;
		repeats = postfixArgDict.repeats;

		score = score[0..endchar-1]; // get rid of the function marker

		// due to Pmono not being able to load a new instr, I check if it there is a new one
		if(agentDict[agent].isNil, {
			agentDict[agent] = [(), ().add(\amp->0.5), []];
		}, {
			if(agentDict[agent][1].instrument != instrument, {
				newInstrFlag = true;
			}, {
			 	newInstrFlag = false;
			});
		});  // 1st = effectRegistryDict, 2nd = scoreInfoDict, 3rd = placeholder for a routine

		if(agentDict[agent][1].pitch.isNil.not and: (pitch != agentDict[agent][1].pitch), {
			newInstrFlag = true;
		});

		// ------------- the envelope amps -------------------
		ampstring = score.tr($ , \).tr($., \);
		amparr = [];
		ampstring.do({arg amp; amparr = amparr.add(amp.asString.asInteger/10) });
		// -------------    the score   -------------------
		quantphase=0;
		spacecount = 0;
		durarr = [];
		score.do({arg char, i;
			if(((char==$ ) || (char==$.)) && (i==0), { startWempty = true; });
			if(((char==$ ) || (char==$.)), {
				spacecount = spacecount+1;
			}, {
				if(i != 0, { // not adding the the first instr
					durarr = durarr.add(spacecount+1); // adding to dur array + include the instr char
					spacecount = 0;
				});
			});
			if(i==(score.size-1), { // fixing at the end of the loop
				durarr = durarr.add(spacecount+1);
				if(startWempty, {
					quantphase = (durarr[0]-1)/4;
					empty = durarr.removeAt(0)-1; // minus the added instr (since we're only interested in spaces)
					durarr[durarr.size-1] = durarr.last+empty; // adding the last space(s) to the first space(s)
				});
			});
		});
		durarr[durarr.size-1] = durarr.last+silences; // adding silences to the end of the score
		durarr = durarr/4;
		durarr = durarr*timestretch; // duration is stretched by timestretch var

		agentDict[agent][1].mode = 2;
		agentDict[agent][1].quantphase = quantphase;
		agentDict[agent][1].durarr = durarr;
		agentDict[agent][1].amparr = amparr;
		agentDict[agent][1].panarr = panarr;
		agentDict[agent][1].pitch = pitch;
		agentDict[agent][1].score = score;
		agentDict[agent][1].repeats = repeats;
		agentDict[agent][1].scorestring = string.asCompileString;
		agentDict[agent][1].instrument = instrument;

		{doc.setStringColor(oncolor, stringstart, stringend-stringstart)}.defer;

//		{doc.setStringColor(oncolor, doc.selectionStart, doc.selectionSize)}.defer(0.1); // if code is green (sleeping)
		"------    ixi lang: Created Concrete Agent : ".post; pureagent.postln; agentDict[agent].postln;
		^this.playScoreMode2(agent, pitch, amparr, durarr, panarr, instrument, quantphase, newInstrFlag, repeats, return, snapshot);
		// this has to be below the playscore method
	}

	parseChord {arg string, mode;
		var splitloc, chordstring, chord, varname;
		var chordstartloc;

		string = string[0..string.size-1].tr($ , \); // get rid of spaces until score
		splitloc = string.find("->");
		varname = string[0..splitloc-1]; // get the name of the var

//		if(mode == \c, {
//			chordstartloc = string.find("(");
//		},{
//			chordstartloc = string.find("�");
//		});
		if(varname.interpret.isInteger, { // PERCUSSIVE MODE ------------ NOT WORKING - Pseq(\instrument does not expand)
			chordstring = string[splitloc+3..string.size-3];
			chord = [];
			chordstring.do({arg instr; chord = chord.add(instrDict[instr.asSymbol]) });
			varDict[varname.asSymbol] = chord;
		}, { // MELODIC MODE
			if(mode == \c, {
				chordstring = string[splitloc+3..string.size-2];
				chord = [];
				chordstring.do({arg note;
					if(note.isAlpha, {
						chord = chord.add(varDict[note.asSymbol]);
					},{
						chord = chord.add(scale[note.asString.asInteger-1]);
					});
				});
				varDict[varname.asSymbol] = chord;
			},{
				chordstring = string[splitloc+3..string.size-1];
				varDict[varname.asSymbol] = chordstring.asInteger;
			});
		});
	}

	createMetaAgent {arg string;
		var splitloc, patternstring, chord, agentname, agent, groupitems, mode;
		var agentarray, tempstring, seq, quant, durarr;
		var repeats = inf;
		var stringstart, stringend, postfixargs, postfixArgDict, endchar;
		var thisline, activeAgentArray;

		splitloc = string.find("->");
		agentname = string[0..splitloc-1]; // get the name of the var
		agentname = agentname.tr($ , \);
		agent = (this.agentPrefix++agentname).asSymbol;
		patternstring = string[splitloc+3..string.size-2].tr($ , $-);
		agentarray = [];
		tempstring = "";
		patternstring.do({arg char;
			if((char != $-) && (char != $~), {
				tempstring = tempstring ++ char;
			}, {
				if(tempstring.size > 0, {
					agentarray = agentarray.add(tempstring);
					tempstring = "";
				});
			});
		});

		endchar = string.findAll("~"); // the index (int) of the end op in the string
		postfixargs = string[endchar[1]+1..string.size-1].tr($ , \); // allowing for spaces
		postfixArgDict = this.parsePostfixArgs(postfixargs);
		repeats = postfixArgDict.repeats;

		"META AGENT CREATED!  NAME: ".post; agentname.postln;

		seq = [];
		activeAgentArray = [];
		groupitems = [];

		(agentarray.size/2).do({arg i;
			var subagent = (this.agentPrefix++agentarray[i*2].asString).asSymbol; // not the new meta agent but its contents
			// activeAgentArray = activeAgentArray.add(agentname);
			activeAgentArray = activeAgentArray.add(subagent);

		// what kind of agent is it? a) agent (mode 0-2), b) meta agent (mode 3), c) a group (mode 4)
			try{
				switch(agentDict[subagent][1].mode)
			{0} {quant = agentDict[subagent][1].durarr.sum; mode = 0 }
			{1} {quant = agentDict[subagent][1].durarr.sum; mode = 1 }
			{2} {quant = agentDict[subagent][1].durarr.sum; mode = 2 }
			{3} {quant = metaAgentDict[subagent].quant; mode = 3 } // it's a meta agent (will play sequentially)
			} {quant = 0; /* TEMP - do I need to set quant? */ mode = 4 }; // it's a group (not meta agent) and will play synchronously)

			groupitems = groupitems.add(agentarray[i*2].asString);
			//this.opInterpreter("@" + agentarray[i*2].asString + agentarray[(i*2)+1], true);
			proxyspace[subagent].clear(0.01);
			seq = seq.add(
				switch(mode)
				{0} {
					durarr = agentDict[subagent][1].durarr;
					this.opInterpreter("@" + agentarray[i*2].asString + agentarray[(i*2)+1], true);
					this.opInterpreter( agentDict[subagent][1].scorestring.tr($",\ ), true );
				}
				{1} {
					durarr = agentDict[subagent][1].durarr;
					this.opInterpreter("@" + agentarray[i*2].asString + agentarray[(i*2)+1], true);
					this.opInterpreter( agentDict[subagent][1].scorestring.tr($",\ ), true );
				}
				{2} {
					durarr = agentDict[subagent][1].durarr;
					this.opInterpreter("@" + agentarray[i*2].asString + agentarray[(i*2)+1], true);
					this.opInterpreter( agentDict[subagent][1].scorestring.tr($",\ ), true );}
				{3} { // meta agent
					durarr = agentDict[subagent][1].durarr;
					Pdef(subagent).source.repeats = agentarray[(i*2)+1].asInteger; // change the repeats argument of the Pseq
					Pdef(subagent);
				}
				{4} { // group (parallel)
					var durarrReady = false;
					var groupPpars = [];
					groups[subagent].do({arg subsubagent;
						if(durarrReady.not, {durarrReady = true; durarr = agentDict[(this.agentPrefix++subsubagent).asSymbol][1].durarr });
						this.opInterpreter("@" + subsubagent + agentarray[(i*2)+1], true);
						groupPpars = groupPpars.add(this.opInterpreter( agentDict[(this.agentPrefix++subsubagent).asSymbol][1].scorestring.tr($",\ ), true ));
					});
					Pdef(subagent, Ppar(groupPpars, 1)); //.quant = [quant, 0];
				};
			);
		});

		groups.add(agent -> groupitems);

		// -- set the general quant argument (from the first agent in a metaagent)
//		if(metaAgentDict.quant.isNil, {
//			metaAgentDict.add(\quant -> quant);
//		});

		// -- create a new agent if needed
		if(agentDict[agent].isNil, {
			agentDict[agent] = [(), ().add(\amp -> 0.5), []];
		});

		if(metaAgentDict[agent].isNil, {
			metaAgentDict[agent] = ().add(\quant -> quant).add(\agents -> activeAgentArray);
		});

		agentDict[agent][1].mode = 3;
	//	agentDict[agent][1].durarr = agentDict[(this.agentPrefix++agentarray[0].asString).asSymbol][1].durarr;
		agentDict[agent][1].durarr = durarr;
		agentDict[agent][1].instrument = "metatrack";
		agentDict[agent][1].scorestring = string;
		agentDict[agent][1].playstate = true;

		Pdef(agent, Pseq(seq, repeats)); //.quant = [quant, 0];

		proxyspace[agent].quant = [quant, 0];
		proxyspace[agent] = Pdef(agent);
		proxyspace[agent].play;

	}

	playScoreMode0 {arg agent, notearr, durarr, instrarr, sustainarr, attackarr, panarr, quantphase, newInstrFlag, morphmode, repeats, return, snapshot;
		var loop, pdef, playNow;
		playNow = return.not; // if returning, then don't play

		if(snapshot.not, {
		metaAgentDict.do({arg metaagent;
			// if the agent is part of a metaAgent it is not played (so we can do "shake ringo", etc. without it being played)
			try{metaagent.agents.do({arg agentindict; if(agentindict == agent, {playNow = false }) })};
		});

		// experimental
//		groups.do({arg group;
//			// if the agent is part of a metaAgent it is not played (so we can do "shake ringo", etc. without it being played)
//			try{group.do({arg agentname; "agentName ".post; if((this.agentPrefix++agentname.asString).asSymbol == agent, { playNow = false }) })};
//		});
		});

		if(snapshot, { playNow = true });

		if(morphmode.isNil, {
			// ------------ play function --------------
			if(proxyspace[agent].isNeutral || (repeats != inf), { // check if the object exists already
				proxyspace[agent].free; // needed because of repeats (free proxyspace timing)
				10.do({arg i; proxyspace[agent][i+1] =  nil }); // oh dear. Proxyspace forces this, as one might want to put an effect again on a repeat pat
				agentDict[agent][0].clear; // clear the effect references
				pdef = Pdef(agent, Pbind(
							\instrument, Pseq(instrarr, inf),
							\midinote, Pseq(notearr, inf),
							\dur, Pseq(durarr, repeats),
							\amp, Pseq(attackarr*agentDict[agent][1].amp, inf),
							\sustain, Pseq(sustainarr, inf),
							\pan, Pseq(panarr, inf)
				));
				if(playNow, {
					{proxyspace[agent].quant = [durarr.sum, quantphase, 0, 1];
					proxyspace[agent].defineBus(numChannels: numChan);
					proxyspace[agent] = Pdef(agent);
					proxyspace[agent].play;}.defer(0.5);
				});
			},{
				if(newInstrFlag, { // only if instrument was {, where Pmono bufferplayer synthdef needs to be shut down (similar to above, but no freeing of effects)
					proxyspace[agent].free; // needed in order to swap instrument in Pmono
					pdef = Pdef(agent, Pbind(
								\instrument, Pseq(instrarr, inf),
								\midinote, Pseq(notearr, inf),
								\dur, Pseq(durarr, repeats),
								\amp, Pseq(attackarr*agentDict[agent][1].amp, inf),
								\sustain, Pseq(sustainarr, inf),
								\pan, Pseq(panarr, inf)
					)).quant = [durarr.sum, quantphase];
					if(playNow, {
						{ proxyspace[agent].play }.defer(0.5); // defer needed as the free above and play immediately doesn't work
					});
				}, {
					// default behavior
					pdef = Pdef(agent, Pbind(
								\instrument, Pseq(instrarr, inf),
								\midinote, Pseq(notearr, inf),
								\dur, Pseq(durarr, repeats),
								\amp, Pseq(attackarr*agentDict[agent][1].amp, inf),
								\sustain, Pseq(sustainarr, inf),
								\pan, Pseq(panarr, inf)
					)).quant = [durarr.sum, quantphase];
					if(playNow, {
						if(agentDict[agent][1].playstate == false, {
							proxyspace[agent].play; // this would build up synths on server on commands such as yoyo agent
						});
					});
				});
			});
		}, {
			if(morphmode.contains("%"), { morphmode = morphmode.tr($%, \); loop = true }, { loop = false }); // check if there is a loop arg or not
			// ------------ play function --------------
			instrarr = instrarr.collect({arg instrname;
				if(ixiInstr.returnBufferDict[instrname.asSymbol].isNil, {
					ixiInstr.returnBufferDict.choose; // if there is a synthesis synth, we have to ignore and we choose ANY buffer instead
				}, {
					ixiInstr.returnBufferDict[instrname.asSymbol];
				})
			});
			if(proxyspace[agent].isNeutral || (repeats != inf), { // check if the object exists alreay
				proxyspace[agent].free; // needed because of repeats (free proxyspace timing)
				10.do({arg i; proxyspace[agent][i+1] =  nil }); // oh dear. Proxyspace forces this, as one might want to put an effect again on a repeat pat
				agentDict[agent][0].clear; // clear the effect references
				pdef = Pdef(agent, Pbind(
							\instrument, morphmode.asSymbol,
							\loop, loop,
							\buf1, Pseq(instrarr, inf),
							\buf2, Pseq(instrarr.rotate(-1), inf),
							\midinote, Pseq(notearr, inf),
							\dur, Pseq(durarr, repeats),
							\amp, Pseq(attackarr*agentDict[agent][1].amp, inf),
							\morphtime, Pseq(durarr/TempoClock.default.tempo, inf),
							//\sustain, Pseq(sustainarr, inf),
							\panFrom, Pseq(panarr, inf),
							\panTo, Pseq(panarr.rotate(-1), inf)
				));
				if(playNow, {
					{proxyspace[agent].quant = [durarr.sum, quantphase, 0, 1];
					proxyspace[agent].defineBus(numChannels: numChan);
					proxyspace[agent] = Pdef(agent);
					proxyspace[agent].play;}.defer(0.5);
				});
			},{
				if(newInstrFlag, { // only if instrument was {, where Pmono bufferplayer synthdef needs to be shut down (similar to above, but no freeing of effects)
					proxyspace[agent].free; // needed in order to swap instrument in Pmono
					pdef = Pdef(agent, Pbind(
							\instrument, morphmode.asSymbol,
							\loop, loop,
							\buf1, Pseq(instrarr, inf),
							\buf2, Pseq(instrarr.rotate(-1), inf),
							\midinote, Pseq(notearr, inf),
							\dur, Pseq(durarr, repeats),
							\amp, Pseq(attackarr*agentDict[agent][1].amp, inf),
							\morphtime, Pseq(durarr/TempoClock.default.tempo, inf),
							//\sustain, Pseq(sustainarr, inf),
							//\pan, Pseq(panarr, inf)
							\panFrom, Pseq(panarr, inf),
							\panTo, Pseq(panarr.rotate(-1), inf)
					)).quant = [durarr.sum, quantphase, 0, 1];
					if(playNow, {
						{ proxyspace[agent].play }.defer(0.5); // defer needed as the free above and play immediately doesn't work
					});
				}, {	// default behavior
					pdef = Pdef(agent, Pbind(
							\instrument, morphmode.asSymbol,
							\loop, loop,
							\buf1, Pseq(instrarr, inf),
							\buf2, Pseq(instrarr.rotate(-1), inf),
							\midinote, Pseq(notearr, inf),
							\dur, Pseq(durarr, repeats),
							\amp, Pseq(attackarr*agentDict[agent][1].amp, inf),
							\morphtime, Pseq(durarr/TempoClock.default.tempo, inf),
							//\sustain, Pseq(sustainarr, inf),
							\panFrom, Pseq(panarr, inf),
							\panTo, Pseq(panarr.rotate(-1), inf)
					)).quant = [durarr.sum, quantphase, 0, 1];
					//proxyspace[agent].play; // this would build up synths on server on commands such as yoyo agent
					if(playNow, {
						if(agentDict[agent][1].playstate == false, {
							proxyspace[agent].play; // this would build up synths on server on commands such as yoyo agent
						});
					});
				});
			});
		});
		agentDict[agent][1].playstate = true;
		if(return, {^pdef});
	}

	playScoreMode1 {arg agent, notearr, durarr, sustainarr, attackarr, panarr, instrument, quantphase, newInstrFlag, midichannel=0, repeats, return, snapshot;
		var pdef, playNow;
		playNow = return.not; // if returning, then don't play
		if(snapshot.not, {
		metaAgentDict.do({arg metaagent;
			// if the agent is part of a metaAgent it is not played (so we can do "shake ringo", etc. without it being played)
			try{metaagent.agents.do({arg agentindict; if(agentindict == agent, {playNow = false }) })};
		});
		// experimental
//		groups.do({arg group;
//			// if the agent is part of a metaAgent it is not played (so we can do "shake ringo", etc. without it being played)
//			try{group.do({arg agentname; "agentName ".post; if((this.agentPrefix++agentname.asString).asSymbol == agent, { playNow = false }) })};
//		});
		});
		if(snapshot, { playNow = true });
		if(instrument.asString=="midi", { eventtype = \midi }, { eventtype = \note });

		// ------------ play function --------------
		if(proxyspace[agent].isNeutral || (repeats != inf), { // check if the object exists alreay
			proxyspace[agent].free; // needed because of repeats (free proxyspace timing)
			10.do({arg i; proxyspace[agent][i+1] =  nil }); // oh dear. Proxyspace forces this, as one might want to put an effect again on a repeat pat
			agentDict[agent][0].clear; // clear the effect references
			pdef = Pdef(agent, Pbind(
						\instrument, instrument,
						\type, eventtype,
						\midiout, midiclient,
						\chan, midichannel,
						\midinote, Pseq(notearr, inf),
						\dur, Pseq(durarr, repeats),
						\sustain, Pseq(sustainarr, inf),
						\amp, Pseq(attackarr*agentDict[agent][1].amp, inf),
						\pan, Pseq(panarr, inf)
			));
			if(playNow, {
				{proxyspace[agent].quant = [durarr.sum, quantphase, 0, 1];
				proxyspace[agent].defineBus(numChannels: numChan);
				proxyspace[agent] = Pdef(agent);
				proxyspace[agent].play;}.defer(0.5);
			});
		},{
			if(newInstrFlag, { // only if instrument was {, where Pmono bufferplayer synthdef needs to be shut down (similar to above, but no freeing of effects)
				proxyspace[agent].free; // needed in order to swap instrument in Pmono
				pdef = Pdef(agent, Pbind(
						\instrument, instrument,
						\type, eventtype,
						\midiout, midiclient,
						\chan, midichannel,
						\midinote, Pseq(notearr, inf),
						\dur, Pseq(durarr, repeats),
						\sustain, Pseq(sustainarr, inf),
						\amp, Pseq(attackarr*agentDict[agent][1].amp, inf),
						\pan, Pseq(panarr, inf)
				)).quant = [durarr.sum, quantphase, 0, 1];
				if(playNow, {
					proxyspace[agent].defineBus(numChannels: numChan);
					{ proxyspace[agent].play }.defer(0.5); // defer needed as the free above and play immediately doesn't work
				});
			}, {
				// default behavior
				pdef = Pdef(agent, Pbind(
						\instrument, instrument,
						\type, eventtype,
						\midiout, midiclient,
						\chan, midichannel,
						\midinote, Pseq(notearr, inf),
						\dur, Pseq(durarr, repeats),
						\sustain, Pseq(sustainarr, inf),
						\amp, Pseq(attackarr*agentDict[agent][1].amp, inf),
						\pan, Pseq(panarr, inf)
				)).quant = [durarr.sum, quantphase, 0, 1];
					//proxyspace[agent].play; // this would build up synths on server on commands such as yoyo agent
				if(playNow, {
					if(agentDict[agent][1].playstate == false, {
						proxyspace[agent].defineBus(numChannels: numChan);
						proxyspace[agent].play; // this would build up synths on server on commands such as yoyo agent
					});
				});
			});
		});
		agentDict[agent][1].playstate = true;
		if(return, {^pdef});
	}

	playScoreMode2 {arg agent, pitch, amparr, durarr, panarr, instrument, quantphase, newInstrFlag, repeats, return, snapshot;
		var pdef, playNow;
		playNow = return.not; // if returning, then don't play
		if(snapshot.not, {
		metaAgentDict.do({arg metaagent;
			// if the agent is part of a metaAgent it is not played (so we can do "shake ringo", etc. without it being played)
			try{metaagent.agents.do({arg agentindict; if(agentindict == agent, {playNow = false }) })};
		});
		// experimental
//		groups.do({arg group;
//			// if the agent is part of a metaAgent it is not played (so we can do "shake ringo", etc. without it being played)
//			try{group.do({arg agentname; "agentName ".post; if((this.agentPrefix++agentname.asString).asSymbol == agent, { playNow = false }) })};
//		});
		});


		if(snapshot, { playNow = true });

		// ------------ play function --------------
		if(proxyspace[agent].isNeutral || (repeats != inf), { // check if the object exists alreay
			proxyspace[agent].free; // needed because of repeats (free proxyspace timing)
			10.do({arg i; proxyspace[agent][i+1] =  nil }); // oh dear. Proxyspace forces this, as one might want to put an effect again on a repeat pat
			agentDict[agent][0].clear; // clear the effect references
			Pdefn((agent++"durarray").asSymbol, Pseq(durarr, repeats));
			Pdefn((agent++"amparray").asSymbol, Pseq(amparr, repeats));
			pdef = Pdef(agent, Pmono(instrument,
						\dur, Pdefn((agent++"durarray").asSymbol),
						\freq, pitch.midicps,
						\noteamp, Pdefn((agent++"amparray").asSymbol),
						\pan, Pseq(panarr, inf)
			));
			if(playNow, {
				{proxyspace[agent].quant = [durarr.sum, quantphase, 0, 1];
				proxyspace[agent].defineBus(numChannels: numChan);
				proxyspace[agent] = Pdef(agent);
				proxyspace[agent].play }.defer(0.5);
			});
		},{
			Pdefn((agent++"durarray").asSymbol, Pseq(durarr, repeats)).quant = [durarr.sum, quantphase, 0, 1];
			Pdefn((agent++"amparray").asSymbol, Pseq(amparr, repeats)).quant = [durarr.sum, quantphase, 0, 1];
			//if(newInstrFlag, { // removed temp for repeat functionality
				proxyspace[agent].free; // needed in order to swap instrument in Pmono
				pdef = Pdef(agent, Pmono(instrument,
							\dur, Pdefn((agent++"durarray").asSymbol),
							\freq, pitch.midicps,
							\noteamp, Pdefn((agent++"amparray").asSymbol),
							\pan, Pseq(panarr, inf)
				));
				if(playNow, {
					{
					proxyspace[agent].defineBus(numChannels: numChan);
					proxyspace[agent] = Pdef(agent);
					proxyspace[agent].play }.defer(0.5); // defer needed as the free above and play immediately doesn't work
				});
			//});
		});
		Pdef(agent).set(\amp, agentDict[agent][1].amp); // proxyspace quirk: amp set from outside
		agentDict[agent][1].playstate = true;
		if(return, {^pdef});
	}

	initEffect {arg string;
		var splitloc, agentstring, endchar, agent, pureagent, effect, effectFoundFlag;
		effectFoundFlag = false;
		string = string.tr($ , \); // get rid of spaces
		string = string.reject({ |c| c.ascii == 10 }); // get rid of char return
		splitloc = string.findAll(">>");
		agent = string[0..splitloc[0]-1];
		pureagent = agent;
		agent = (this.agentPrefix++agent).asSymbol;
		string = string++$ ;
		endchar = string.find(" ");
		splitloc = splitloc.add(endchar);
		if(groups.at(agent).isNil.not, { // the "agent" is a group or a metaAgent
			effect = string[splitloc[0]..splitloc.last];
			groups.at(agent).do({arg agentx, i;
				this.initEffect(agentx++effect); // recursive calling of this method
			});
		}, { // it is a real agent, not a group, then we ADD THE EFFECT
			(splitloc.size-1).do({arg i;
				effect = string[splitloc[i]+2..splitloc[i+1]-1];
				if(agentDict[agent][0][effect.asSymbol].isNil, { // experimental - only add if there is no effect
					agentDict[agent][0][effect.asSymbol] = agentDict[agent][0].size+1;// add 1 (the source is 1)
					if(effectDict[effect.asSymbol].isNil.not, {
						effectFoundFlag = true;
						proxyspace[agent][agentDict[agent][0].size] = \filter -> effectDict[effect.asSymbol];
					});
				});
			});
			" --->    ixi lang : AGENT/GROUP _%_ HAS THE FOLLOWING EFFECT(S) : ".postf(pureagent); agentDict[agent][0].postln;
		});
		if(metaAgentDict.at(agent).isNil.not, {
			(splitloc.size-1).do({arg i;
				effect = string[splitloc[i]+2..splitloc[i+1]-1];
				if(agentDict[agent][0][effect.asSymbol].isNil, { // experimental - only add if there is no effect
					agentDict[agent][0][effect.asSymbol] = agentDict[agent][0].size+1;// add 1 (the source is 1)
					if(effectDict[effect.asSymbol].isNil.not, {
						effectFoundFlag = true;
						proxyspace[agent][agentDict[agent][0].size] = \filter -> effectDict[effect.asSymbol];
					});
				});
			});
		});

	}

	removeEffect {arg string;
		var splitloc, agentstring, endchar, agent, pureagent, effect, effectFoundFlag;
		effectFoundFlag = false;
		string = string.tr($ , \); // get rid of spaces
		string = string.reject({ |c| c.ascii == 10 });
 		splitloc = string.findAll("<<");
		agent = string[0..splitloc[0]-1];
		pureagent = agent;
		agent = (this.agentPrefix++agent).asSymbol;
		string = string++$ ;
		endchar = string.find(" ");
		splitloc = splitloc.add(endchar);
		if(groups.at(agent).isNil.not, { // the "agent" is a group
			effect = string[splitloc[0]..splitloc.last];
			groups.at(agent).do({arg agentx, i;
				this.removeEffect(agentx++effect); // recursive calling of same method
			});
		}, { // it is a real agent, not a group
			if(splitloc[0]==(endchar-2), { // remove all effects (if only << is passed)
				" --->    ixi lang : REMOVE ALL EFFECTS".postln;
				10.do({arg i; proxyspace[agent][i+1] =  nil }); // remove all effects (10 max) (+1 as 0 is Pdef)
				agentDict[agent][0].clear;
			}, { // only remove the effects listed (such as agent<<reverb<<tremolo)
				(splitloc.size-1).do({arg i;
					effect = string[splitloc[i]+2..splitloc[i+1]-1];
					if(agentDict[agent][0][effect.asSymbol].isNil.not, { // if the effect exists
						proxyspace[agent][ (agentDict[agent][0][effect.asSymbol]).clip(1,10)] =  nil;
						agentDict[agent][0].removeAt(effect.asSymbol);
					});
				});
			});
			" --->    ixi lang : AGENT/GROUP _%_ HAS THE FOLLOWING EFFECT(S) : ".postf(pureagent); agentDict[agent][0].postln;
		});
		if(metaAgentDict.at(agent).isNil.not, {
			if(splitloc[0]==(endchar-2), { // remove all effects (if only << is passed)
				" --->    ixi lang : REMOVE ALL EFFECTS".postln;
				10.do({arg i; proxyspace[agent][i+1] =  nil }); // remove all effects (10 max) (+1 as 0 is Pdef)
				agentDict[agent][0].clear;
			}, { // only remove the effects listed (such as agent<<reverb<<tremolo)
				(splitloc.size-1).do({arg i;
					effect = string[splitloc[i]+2..splitloc[i+1]-1];
					if(agentDict[agent][0][effect.asSymbol].isNil.not, { // if the effect exists
						proxyspace[agent][ (agentDict[agent][0][effect.asSymbol]).clip(1,10)] =  nil;
						agentDict[agent][0].removeAt(effect.asSymbol);
					});
				});
			});
		});
	}

	increaseAmp {arg string;
		var splitloc, agentstring, endchar, agent, effect, effectFoundFlag, amp, dict;
		effectFoundFlag = false;
		if(string.find("))") == 0, { string = string.tr($), \).tr($ ,\) ++ "))"});  // if using the verb-agent structure
		string = string.tr($ , \); // get rid of spaces
		splitloc = string.find("))");
		agentstring = string[0..splitloc-1]; // get the name of the agent
		agent = agentstring[0..agentstring.size-1];
		agent = (this.agentPrefix++agent).asSymbol;
		dict = agentDict[agent][1];
		if(groups[agent].isNil.not, { // the "agent" is a group
			groups[agent].do({arg agentx, i;
				this.increaseAmp(agentx++"))"); // recursive calling of this same method
			});
		}, {
			amp = agentDict[agent][1].amp;
			amp = (amp + 0.05).clip(0, 2);
			(" --->    ixi lang : AMP in agent "++agent.asString++" : ").post; amp.postln;
			agentDict[agent][1].amp = amp;
			switch(agentDict[agent][1].mode)
				{0} { this.playScoreMode0(agent, dict.notearr, dict.durarr, dict.instrarr, dict.sustainarr, dict.attackarr, dict.panarr,
					dict.quantphase, false, dict.morphmode, dict.repeats, false, false); }
				{1} { this.playScoreMode1(agent, dict.notearr, dict.durarr, dict.sustainarr, dict.attackarr, dict.panarr, dict.instrument,
					dict.quantphase, false, dict.midichannel, dict.repeats, false, false); }
				{2} { Pdef(agent).set(\amp, amp) };
		});
	}

	decreaseAmp {arg string;
		var splitloc, agentstring, endchar, agent, effect, effectFoundFlag, amp, dict;
		effectFoundFlag = false;
		if(string.find("((") == 0, { string = string.tr($(, \).tr($ ,\) ++ "(("}); // if using the verb-agent structure
		string = string.tr($ , \); // get rid of spaces
		splitloc = string.find("((");
		agentstring = string[0..splitloc-1]; // get the name of the agent
		agent = agentstring[0..agentstring.size-1];
		agent = (this.agentPrefix++agent).asSymbol;
		dict = agentDict[agent][1];
		if(groups[agent].isNil.not, { // the "agent" is a group
			groups[agent].do({arg agentx, i;
				this.decreaseAmp(agentx++"(("); // recursive calling of this same method
			});
		}, {
			amp = agentDict[agent][1].amp;
			amp = (amp - 0.05).clip(0, 2);
			(" --->    ixi lang : AMP in agent "++agent.asString++" : ").post; amp.postln;
			agentDict[agent][1].amp = amp;
			switch(agentDict[agent][1].mode)
				{0} { this.playScoreMode0(agent, dict.notearr, dict.durarr, dict.instrarr, dict.sustainarr, dict.attackarr, dict.panarr,
					dict.quantphase, false, dict.morphmode, dict.repeats, false, false); }
				{1} { this.playScoreMode1(agent, dict.notearr, dict.durarr, dict.sustainarr, dict.attackarr, dict.panarr, dict.instrument,
					dict.quantphase, false, dict.midichannel, dict.repeats, false, false); }
				{2} { Pdef(agent).set(\amp, amp) };
		});
	}

	findStringStartEnd {arg doc, pureagentname;
		var allreturns, stringstart, stringend, tempstringstart;
		var docstring = doc.string ++ "\n"; // added in Qt as the last line wasn't working
//		allreturns = doc.string.findAll("\n") ++ doc.string.findAll(""++13.asAscii);
		allreturns = docstring.findAll("\n") ++ docstring.findAll(""++13.asAscii);
		allreturns.notNil.if({
			try{block{ | break |
				doc.string.findAll(pureagentname).do({arg loc, i;
					stringend = allreturns[allreturns.indexOfGreaterThan(loc)];
					if(doc.string[(loc+pureagentname.size)..stringend].contains("->"), {
						tempstringstart = loc; //doc.string.find(pureagentname);
						if(pureagentname == doc.string[tempstringstart..doc.string.findAll("->")[doc.string.findAll("->").indexOfGreaterThan(tempstringstart+1)]-1].tr($ , \), {
							// the line below will check if it's the same agent in the dict and on the doc. if so, it changes it (in case there are many with same name)
							//stringend.isNil.if({stringend = 0});
							if(agentDict[(this.agentPrefix++pureagentname.asString).asSymbol][1].scorestring == doc.string[loc..stringend-1].asCompileString, {
								stringstart = tempstringstart;
								break.value; //  exact match found and we break loop, leaving stringstart and stringend with correct values
							});
						});
					});
				});
			}};
			// if an EXACT agent score has not been found, then apply the action upon an agent with the same name (can happen if future is running and score is changed)
			if(stringstart == nil, {
				block{ | break |
					doc.string.findAll(pureagentname).do({arg loc, i;
						stringend = allreturns[allreturns.indexOfGreaterThan(loc)];
						if(doc.string[(loc+pureagentname.size)..stringend].contains("->"), {
							stringstart = loc; //doc.string.find(pureagentname);
							if(pureagentname == doc.string[stringstart..doc.string.findAll("->")[doc.string.findAll("->").indexOfGreaterThan(stringstart+1)]-1].tr($ , \), {
								break.value; //  exact match found and we break loop, leaving stringstart and stringend with correct values
							});
						});
					});
				};
			});
		});
		^[stringstart, stringend];
	}

	// modearray is an array with three booleans stating if the method can be applied on that mode
	// newarg is an array with the method and the value(s)
	swapString {arg doc, pureagentname, newscore, modearray, newarg, return=false;
		var allreturns, stringstart, stringend, cursorPos, modstring, thisline;
		var scorestringsuffix, argsuffix, scoremode, scorerange, score, agent;
		var val, end;
		var newstr;
		#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);
		thisline = doc.string[stringstart..stringend];

		// TEST XXX (For scheme like methods - e.g.,  + agent 12)
		// if new argument then add that to the line
		if(newarg.isNil.not, {
			if(thisline.contains(newarg[0]), {
				if((newarg[0] == "-") , { // ouch - the assignment operator contains a "-" so I need specific rule for subtraction
					if(thisline.findAll("-").size > 1, {
						block{|break|
							thisline[thisline.findAll("-")[1]+1..thisline.size-1].do({arg item, i;
								if(item.isAlphaNum || (item == $.), {val = val ++ item }, {end = thisline.findAll("-")[1]+1+i; break.value});
							});
						};
						thisline = thisline[0..thisline.findAll("-")[1]]++newarg[1]++thisline[end..thisline.size-1]; // ++"\n";
					}, {
						thisline = thisline++newarg[0]++newarg[1]; // no need to replace an operator, just add to the end
					});
				}, { // this is the MAIN method for replacing operators (for all but "-" as seen above)
					block{|break|
						thisline[thisline.find(newarg[0])+1..thisline.size-1].do({arg item, i;
							if(item.isAlphaNum || (item == $_) || (item == $.), {val = val ++ item }, {end = thisline.find(newarg[0])+1+i; break.value});
						});
					};

					switch(newarg[0])
					{"^"} {
						thisline = thisline[0..thisline.find(newarg[0])]++newarg[1]++newarg[0]++thisline[end..thisline.size-1]; // ++"\n";
						thisline = thisline.replace("^^", "^");
					}
					{"("} {
						thisline = thisline[0..thisline.find(newarg[0])]++newarg[1]++")"++thisline[end..thisline.size-1]; // ++"\n";
						thisline = thisline.replace("((", "(").replace("))", ")");
					}
					{"<"} {
						thisline = thisline[0..thisline.find(newarg[0])]++newarg[1]++">"++thisline[end..thisline.size-1]; // ++"\n";
						thisline = thisline.replace("<<", "<").replace(">>", ">");
					}
					{
						thisline = thisline[0..thisline.find(newarg[0])]++newarg[1]++thisline[end..thisline.size-1]; // ++"\n";
					};
				});
			},{
				switch(newarg[0])
					{"^"} {
						thisline = thisline++newarg[0]++newarg[1]++newarg[0]; // no need to replace an operator, just add to the end
					}
					{"("} {
						thisline = thisline++newarg[0]++newarg[1]++")"; // no need to replace an operator, just add to the end
					}
					{"<"} {
						thisline = thisline++newarg[0]++newarg[1]++">"; // no need to replace an operator, just add to the end
					}
					{
						thisline = thisline++newarg[0]++newarg[1]; // no need to replace an operator, just add to the end
					};
			});
		});

		agent = (this.agentPrefix++pureagentname).asSymbol;

		if(agentDict.keys.includes(agent), {
			if(thisline.find("|").isNil.not, {
				scorerange = thisline.findAll("|");
				argsuffix = thisline[scorerange[1]+1..thisline.size-1]; // if arguments are added
				scoremode = 0;
			});
			if(thisline.find("[").isNil.not, {
				scorerange = [];
				scorerange = scorerange.add(thisline.find("["));
				scorerange = scorerange.add(thisline.find("]"));
				argsuffix = thisline[scorerange[1]+1..thisline.size-1]; // if arguments are added
				scoremode = 1;
			});
			if(thisline.find("{").isNil.not, {
				scorerange = [];
				scorerange = scorerange.add(thisline.find("{"));
				scorerange = scorerange.add(thisline.find("}"));
				argsuffix = thisline[scorerange[1]+1..thisline.size-1]; // if arguments are added
				scoremode = 2;
			});

			if(thisline.find("~").isNil.not, {
				scorerange = thisline.findAll("~");
				argsuffix = thisline[scorerange[1]+1..thisline.size-1]; // if arguments are added
				scoremode = 3;
					{ // make agent white again
						cursorPos = doc.selectionStart; // get cursor pos
						#stringstart, stringend = this.findStringStartEnd(doc, pureagentname); // this will cause error since the agent string will have changed
						// -- doc.setStringColor(oncolor, stringstart, stringend-stringstart);
						//doc.selectRange(cursorPos); // set cursor pos again
					doc.select(cursorPos, 0);
					}.defer;
			});

			if(modearray[scoremode], { // if this scoremode supports the operation (no need to yoyo a melodic score for example)

				score = thisline[scorerange[0]+1..scorerange[1]-1];
				scorestringsuffix = switch(scoremode) {0}{"|"++argsuffix}{1}{"]"++argsuffix}{2}{"}"++argsuffix}{3}{"~"++argsuffix};
				// -------- put it back in place ----------
				modstring = thisline[0..scorerange[0]]++newscore++scorestringsuffix;
				modstring = modstring.reject({ |c| c.ascii == 10 }); // get rid of \n
				{
					{ // make the agent yellow
						cursorPos = doc.selectionStart; // get cursor pos
						#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);
						(stringend.notNil && stringstart.notNil).if({
							doc.setStringColor(activecolor, stringstart, stringend-stringstart);
						});
						//doc.selectRange(cursorPos); // set cursor pos again
						doc.select(cursorPos, 0);
					}.defer;
					0.3.wait;
					{ // swap agent's strings
						cursorPos = doc.selectionStart; // get cursor pos
						#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);
						(stringend.notNil && stringstart.notNil).if({
							newstr = doc.string[0..stringstart-1] ++ modstring ++ doc.string[stringend..doc.string.size];
							//doc.string_(newstr);
							doc.setString(modstring, stringstart, stringend-stringstart);
							doc.select(cursorPos, 0);
							/*doc.string_( modstring, stringstart, stringend-stringstart);*/
							//doc.string_(doc.string + "\n" ++ modstring);
						});

						/*
						// for methods that change string sizes, it's good to do the below so cursor is placed correctly
						// if cursorPos < stringend, then the same location, else calcultate new location cf. new string size
						if(cursorPos<stringend, {
							//doc.selectRange(cursorPos); // set cursor pos again
						}, {
							//doc.selectRange(cursorPos+(modstring.size-(stringend-stringstart))); // set cursor pos again
						});
						*/
						switch(scoremode)
							{0} { if(agentDict[agent][1].playstate, {this.parseScoreMode0(modstring, return)}) }
							{1} { if(agentDict[agent][1].playstate, {this.parseScoreMode1(modstring, return)}) }
							{2} { if(agentDict[agent][1].playstate, {this.parseScoreMode2(modstring, return)}) }
						//	{3} { if(agentDict[agent][1].playstate, {this.parseScoreMode3(modstring, return)}, {this.parseScoreMode3(modstring, return)}) }

					}.defer;
					0.3.wait;
					{ // make agent white again
						cursorPos = doc.selectionStart; // get cursor pos
						#stringstart, stringend = this.findStringStartEnd(doc, pureagentname); // this will cause error since the agent string will have changed
						(stringend.notNil && stringstart.notNil).if({
							//doc.setStringColor(oncolor, stringstart, stringend-stringstart);
						});
						//doc.selectRange(cursorPos); // set cursor pos again
						doc.select(cursorPos, 0);
					}.defer;

				}.fork(TempoClock.new);

			},{
				"ixi lang ERROR: Action not applicable in this mode".postln;
			});
		},{
				"ixi lang ERROR: Agent with this name not found!!!".postln;
		});
	}

	parseMethod {arg string, return=false;
		var splitloc, methodstring, spaces, agent, method, pureagentname;
		var thisline, modstring, stringstart, allreturns, stringend, scorerange, score, scoremode, scorestringsuffix;
		var argument, argsuffix, cursorPos;

		splitloc = string.find(" ");
		methodstring = string[0..splitloc-1].tr($ , \); // get the name of the agent
		method = methodstring[0..methodstring.size-1];
		string = string.reject({ |c| c.ascii == 10 }); // get rid of \n
		string = string++" "; // add a space to the end
		spaces = string.findAll(" ");
		agent = string[splitloc+1..spaces[1]-1];
		pureagentname = agent; // the name of the agent is different in code (0john) and in doc (john) (for multidoc support)
		agent = (this.agentPrefix++agent).asSymbol;
		if( spaces.size > 1, { argument = string[spaces[1]..spaces[spaces.size-1]] }); // is there an argument?


		// HERE CHECK IF IT'S A GROUP THEN PERFORM A DO LOOP (for each member of the group)
		if(groups.at(agent).isNil.not, { // the "agent" is a group
			groups.at(agent).do({arg agentx, i;
				this.parseMethod(method+agentx+argument); // recursive calling of this method
			});
			if(metaAgentDict.at(agent).isNil.not, { // metaagents are always groups as well.
				// Only "doze" and "perk" are relevant for meta agent control
				// the rest of the methods apply to the sub-agents
				#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);
				switch(method)
					{"doze"} {// pause stream
						if(agentDict[agent][1].playstate == true, {
							agentDict[agent][1].playstate = false;
							proxyspace[agent].stop;
						(stringend.notNil && stringstart.notNil).if({
							doc.setStringColor(offcolor, stringstart, stringend-stringstart);
						});
						});
					}
					{"perk"} { // restart stream
						if(agentDict[agent][1].playstate == false, {
							agentDict[agent][1].playstate = true;
							proxyspace[agent].play;
						(stringend.notNil && stringstart.notNil).if({
					 		doc.setStringColor(oncolor, stringstart, stringend-stringstart);
						});
						});
					}
					{"nap"} { // pause for either n secs or n secs:number of times
						var napdur, separator, times, on, barmode;
						on = true;
						separator = argument.find(":");

						if(separator.isNil.not, { // it contains a on/off order
							napdur = argument[0..separator-1].asFloat;
							// round to even, so it doesn't leave the stream off
							times = argument[separator+1..argument.size-1].asInteger.round(2);
						 	{
						 		(times*2).do({ // times two as the interface is that it should nap twice
						 			if(on, {
						 				proxyspace[agent].objects[0].array[0].mute;
						 				agentDict[agent][1].playstate = false;
									(stringend.notNil && stringstart.notNil).if({
								 		{doc.setStringColor(offcolor, stringstart, stringend-stringstart)}.defer;
									});
						 				on = false;
						 			}, {
						 				proxyspace[agent].objects[0].array[0].unmute;
										agentDict[agent][1].playstate = true;
										(stringend.notNil && stringstart.notNil).if({
											{doc.setStringColor(oncolor, stringstart, stringend-stringstart)}.defer;
										});
						 				on = true;
						 			});
						 			napdur.wait;
					 			});
						 	}.fork(TempoClock.new);
						}, { // it is just a nap for n seconds and then reawake
						 	{
								napdur = argument.asFloat;
					 			proxyspace[agent].objects[0].array[0].mute;
								(stringend.notNil && stringstart.notNil).if({
									{doc.setStringColor(offcolor, stringstart, stringend-stringstart)}.defer;
								});
								napdur.wait;
					 			proxyspace[agent].objects[0].array[0].unmute;
								(stringend.notNil && stringstart.notNil).if({
									{doc.setStringColor(oncolor, stringstart, stringend-stringstart)}.defer;
								});
						 	}.fork(TempoClock.new);
					 	});
					};
			});
		}, { // it is a real agent, not a group

			// -------- find the line in the document -----------
			#stringstart, stringend = this.findStringStartEnd(doc, pureagentname);

			thisline = doc.string[stringstart..stringend];
			// -------- detect which mode it is ---------------
			if(thisline.find("|").isNil.not, {
				scorerange = thisline.findAll("|");
				argsuffix = thisline[scorerange[1]+1..thisline.size-1]; // if transposition is added
				scoremode = 0;
			});
			if(thisline.find("[").isNil.not, {
				scorerange = [];
				scorerange = scorerange.add(thisline.find("["));
				scorerange = scorerange.add(thisline.find("]"));
				argsuffix = thisline[scorerange[1]+1..thisline.size-1]; // if transposition is added
				scoremode = 1;
			});
			if(thisline.find("{").isNil.not, {
				scorerange = [];
				scorerange = scorerange.add(thisline.find("{"));
				scorerange = scorerange.add(thisline.find("}"));
				scoremode = 2;
			});
			if(thisline.find("~").isNil.not, {
				scorerange = thisline.findAll("~");
				argsuffix = thisline[scorerange[1]+1..thisline.size-1]; // if transposition is added
				scoremode = 3;
			});
			score = thisline[scorerange[0]+1..scorerange[1]-1];
			scorestringsuffix = switch(scoremode) {0}{"|"++argsuffix}{1}{"]"++argsuffix}{2}{"}"};

			// -------------   Perform methods - the ixi lang verbs
			switch(method)
				{"doze"} {// pause stream
					if(agentDict[agent][1].playstate == true, {
						agentDict[agent][1].playstate = false;
						//if(agentDict[agent][1].mode == 2, { // mode 2 would not mute/unmute
							//Pdef(agent).stop;
							proxyspace[agent].stop;
						//});
						//proxyspace[agent].objects[0].array[0].mute;
						(stringend.notNil && stringstart.notNil).if({
							doc.setStringColor(offcolor, stringstart, stringend-stringstart);
						});
					});
				}
				{"perk"} { // restart stream
					if(agentDict[agent][1].playstate == false, {
						agentDict[agent][1].playstate = true;
						//if(agentDict[agent][1].mode == 2, { // mode 2 would not mute/unmute
							//Pdef(agent).play;
							proxyspace[agent].play;
						//});
						//proxyspace[agent].objects[0].array[0].unmute;
						(stringend.notNil && stringstart.notNil).if({
							doc.setStringColor(oncolor, stringstart, stringend-stringstart);
						});
					});
				}
				{"nap"} { // pause for either n secs or n secs:number of times
					var napdur, separator, times, on, barmode;
					on = true;
					separator = argument.find(":");

					if(separator.isNil.not, { // it contains a on/off order
						if(argument.contains("b") || argument.contains("B"), {
							barmode = true;
							napdur = argument[0..separator-1].tr($b, \).asFloat;
						},{
							barmode = false; // is the future working in seconds or bars ?
							napdur = argument[0..separator-1].asFloat;
						});
						//napdur = argument[0..separator-1].asInteger;
						// round to even, so it doesn't leave the stream off
						times = argument[separator+1..argument.size-1].asInteger.round(2);
					 	{
					 		(times*2).do({ // times two as the interface is that it should nap twice
					 			if(on, {
									if(agentDict[agent][1].mode == 2, { // mode 2 would not mute/unmute
										proxyspace[agent].stop;
									});
					 				try{proxyspace[agent].objects[0].array[0].mute}; // inside try, as metaagents have to nap as well
					 				agentDict[agent][1].playstate = false;
									(stringend.notNil && stringstart.notNil).if({
										{doc.setStringColor(offcolor, stringstart, stringend-stringstart)}.defer;
									});
					 				on = false;
					 			}, {
									if(agentDict[agent][1].mode == 2, { // mode 2 would not mute/unmute
										proxyspace[agent].play;
									});
					 				try{proxyspace[agent].objects[0].array[0].unmute}; // inside try, as metaagents have to nap as well
									agentDict[agent][1].playstate = true;
										(stringend.notNil && stringstart.notNil).if({
											{doc.setStringColor(oncolor, stringstart, stringend-stringstart)}.defer;
										});
					 				on = true;
					 			});

								if(barmode, {
								      ((napdur*agentDict[agent][1].durarr.sum)/TempoClock.default.tempo).wait;
								},{
								      napdur.wait;
								});
				 			});
					 	}.fork(TempoClock.new);
					}, { // it is just a nap for n seconds and then reawake
					 	{
//							argument = argument.asInteger;
							if(argument.contains("b") || argument.contains("B"), {
								barmode = true;
								napdur = argument.tr($b, \).asFloat;
							},{
								barmode = false; // is the future working in seconds or bars ?
								napdur = argument.asFloat;
							});
																			if(agentDict[agent][1].mode == 2, { // mode 2 would not mute/unmute
								proxyspace[agent].stop;
							});

					 		try{proxyspace[agent].objects[0].array[0].mute};
								(stringend.notNil && stringstart.notNil).if({
							{doc.setStringColor(offcolor, stringstart, stringend-stringstart)}.defer;
								});
							if(barmode, {
							      ((napdur*agentDict[agent][1].durarr.sum)/TempoClock.default.tempo).wait;
							},{
							      napdur.wait;
							});
							if(agentDict[agent][1].mode == 2, { // mode 2 would not mute/unmute
								proxyspace[agent].play;
							});
					 		try{proxyspace[agent].objects[0].array[0].unmute};
								(stringend.notNil && stringstart.notNil).if({
							{doc.setStringColor(oncolor, stringstart, stringend-stringstart)}.defer;
								})
					 	}.fork(TempoClock.new);
				 	});
				}
				{"shake"} {
					// -------- perform the method -----------
					score = score.scramble;
					// -------- put it back in place ----------
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"replace"} {
					var chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
					// -------- perform the method -----------
					if(scoremode == 0, {
						if(argument.contains("down"), {
							score = score.collect({arg char; if(char != Char.space, {chars[26..51].choose}, {char})});
						}, {
							if(argument.contains("up"), {
								score = score.collect({arg char; if(char != Char.space, {chars[0..26].choose}, {char})});
							}, {
								score = score.collect({arg char; if(char != Char.space, {chars.choose}, {char})});
							});
						});
					}, {
						score = score.collect({arg char; if(char.isAlphaNum, {9.rand}, {char})});
					});
					// -------- put it back in place ----------
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"insert"} {
					var chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
					// -------- perform the method -----------
					if(scoremode == 0, {
						score = score.collect({arg char; if((char == Char.space) && 0.5.coin, {chars.choose}, {char})});
					}, {
						score = score.collect({arg char; if((char == Char.space) && 0.5.coin, {9.rand}, {char})});
					});
					// -------- put it back in place ----------
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"remove"} {
					// -------- perform the method -----------
					if(scoremode == 0, {
						score = score.collect({arg char; if((char != Char.space) && 0.5.coin, {Char.space}, {char}) });
					}, {
						score = score.collect({arg char; if((char != Char.space) && 0.5.coin, {Char.space}, {char}) });
					});
					// -------- put it back in place ----------
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"swap"} {
					var instruments;
					// -------- perform the method -----------
					instruments = score.reject({arg c; c==$ }).scramble;
					score = score.collect({arg char; if(char!=$ , {instruments.pop}, {" "}) });

					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{">shift"} {
					argument = argument.asInteger;
					// -------- perform the method -----------
					score = score.rotate(if((argument==0) || (argument=="") || (argument==" "), {1}, {argument}));
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"<shift"} {
					argument = argument.asInteger;
					// -------- perform the method -----------
					score = score.rotate(if((argument==0) || (argument=="") || (argument==" "), {-1}, {argument.neg}));
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"invert"} {
					var temparray;
					// -------- perform the method -----------
					temparray = [];
					score.do({arg char; temparray = temparray.add( if(char==$ , {nil}, {char.asString.asInteger}) ) });
					temparray = temparray.collect({arg val; if(val.isNil.not, { abs(val-8) }) });
					score = "";
					temparray.do({arg item; score = score++if(item==nil, {" "}, {item.asString}) });
					this.swapString(doc, pureagentname, score, [false, true, true]);
				}
				{"expand"} { // has to behave differently as it adds characters
					var tempstring;
					argument = argument.asInteger;
					// -------- perform the method -----------
					if(argument.isNil || (argument==" "), {1}, {argument});
					tempstring = "";
					score.do({arg char;
						tempstring = tempstring++char;
						if(char!=$ , {
							argument.do({ tempstring = tempstring++" " });
						});
					});
					score = tempstring;
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"revert"} { // reverse
					// -------- perform the method -----------
					score = score.reverse;
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				{"up"} { // all instruments uppercase
					// -------- perform the method -----------
					score = score.toUpper;
					this.swapString(doc, pureagentname, score, [true, false, false]);
				}
				{"down"} { // all instruments lowercase
					// -------- perform the method -----------
					score = score.toLower;
					this.swapString(doc, pureagentname, score, [true, false, false]);
				}
				{"yoyo"} { // swaps lowercase and uppercase randomly
					// -------- perform the method -----------
					score = score.collect({arg char; 0.5.coin.if({char.toUpper},{char.toLower})});
					this.swapString(doc, pureagentname, score, [true, false, false]);
				}
				{"beer"} {
					var durarr, sum, copy, summed, newdurs;
					argument = if(argument.asFloat == 0, { 0.1 }, { argument.asFloat/10 });
					// drunk
					durarr = agentDict[agent][1].durarr;
					sum = durarr.sum;
					copy = [];
					durarr.do({arg item, i; copy = copy.add(item+(argument.rand2)) });
					summed = copy.asArray.normalizeSum * sum;
					durarr.size.do({arg i; durarr[i] = copy[i] });
				}
				{"hash"} {
					argument = if(argument.asFloat == 0, { 0.1 }, { argument.asFloat/10 });
					Pdef(agent.asSymbol).align([argument, argument, argument])
					// -------- perform the method -----------
				}
				{"coffee"} {
					argument = if(argument.asFloat == 0, { -0.01 }, { (argument.asFloat/100)* -1 });
					Pdef(agent.asSymbol).align([argument, argument])
				}
				{"LSD"} {
					var durarr, sum, copy, summed, newdurs, last, arraybutlastsum;
					argument = if(argument.asFloat == 0, { 0.1 }, { argument.asFloat/10 });
					durarr = agentDict[agent][1].durarr;
					sum = durarr.sum;
					copy = [];
					durarr.do({arg item, i; copy = copy.add(item+(argument.rand)) });
					//summed = copy.asArray.normalizeSum * sum;
					arraybutlastsum = copy[0..copy.size-2].sum;
					last = sum-arraybutlastsum;
					copy[copy.size-1] = last;
					durarr.size.do({arg i; durarr[i] = abs(copy[i]) }); // make it absolote, so in the end (if too spaced) timing suffers
				}
				{"detox"} {
					this.swapString(doc, pureagentname, score, [true, true, true]);
				}
				// below are Scheme like methods with operator in front of the agent

				{"))"} { // in future mode, agents can increase and decrease volume
					this.increaseAmp(pureagentname++"))");
				}
				{"(("} { // put things in order in time
					this.decreaseAmp(pureagentname++"((");
				}
				{"+"} {
					argument = argument.asFloat;
					// -------- perform the method -----------
					this.swapString(doc, pureagentname, score, [true, true, true], ["+", argument]);
				}
				{"-"} {
					argument = argument.asFloat;
					// -------- perform the method -----------
					this.swapString(doc, pureagentname, score, [true, true, true], ["-", argument]);
				}
				{"*"} {
					argument = argument.asFloat;
					// -------- perform the method -----------
					this.swapString(doc, pureagentname, score, [true, true, true], ["*", argument]);
				}
				{"/"} {
					argument = argument.asFloat;
					// -------- perform the method -----------
					this.swapString(doc, pureagentname, score, [true, true, true], ["/", argument]);
				}
				{"!"} {
					argument = argument.asInteger;
					// -------- perform the method -----------
					this.swapString(doc, pureagentname, score, [true, true, true, false], ["!", argument]);
				}
				{"("} {
					//argument = argument.asInteger;
					argument = argument.tr($ ,\);
					// -------- perform the method -----------
					this.swapString(doc, pureagentname, score, [true, true, true, false], ["(", argument]);
				}
				{"^"} {
					argument = argument.asInteger;
					// -------- perform the method -----------
					this.swapString(doc, pureagentname, score, [true, true, true, false], ["^", argument]);
				}
				{"<"} {
					argument = argument.asInteger;
					// -------- perform the method -----------
					this.swapString(doc, pureagentname, score, [true, true, true, false], ["<", argument]);
				}
				{"@"} {
					argument = argument.asInteger;
					// -------- perform the method -----------
					this.swapString(doc, pureagentname, score, [true, true, true, true], ["@", argument], return);
				}
				{"order"} { // put things in order in time

				}
				{"remind"} {
					this.getMethodsList;
				}
				{"input"} {
					inbus = argument.asInteger + 7; // for live recording of samples
				}
				{"scale"} {
					var firstarg, secondarg, chosenscale, globalscale, dictscore, agent, mode, tempscale;

					firstarg = string[string.findAll(" ")[0]+1..(string.findAll(" ")[1])-1];
					agent = (this.agentPrefix++firstarg).asSymbol;
					secondarg = string[string.findAll(" ")[1]+1..(string.findAll(" ")[2])-1];
					chosenscale = ("Scale."++secondarg).interpret;
					chosenscale.tuning_(tuning.asSymbol);
					tempscale = scale; // store the default scale of the session
					scale = chosenscale.semitones.copy.add(12); // used to be degrees, but that doesn't support tuning

					dictscore = agentDict[agent][1].scorestring;
					dictscore = dictscore.reject({ |c| c.ascii == 34 }); // get rid of quotation marks
					mode = block{|break|
						["|", "[", "{", ")"].do({arg op, i;						var c = dictscore.find(op);
							if(c.isNil.not, {break.value(i)});
						});
					};
					switch(mode)
						{0} { this.parseScoreMode0(dictscore, false) }
						{1} { this.parseScoreMode1(dictscore, false) };
					//	{2} { this.parseScoreMode2(dictscore) }; // not relevant
					if(agentDict[agent][1].playstate == true, {
						proxyspace[agent].play;
					});
					scale = tempscale; // set global scale of the session back to the default
				}
				;
		});
	}

	updateInstrDict {arg char;
		instrDict[char.asSymbol] = char.asSymbol;
		//instrDict = dict;
	}


	getMethodsList {
		// var doc, str;
		//doc = Document.new;
		//doc.name_("ixi lang lingo");
		//doc.promptToSave_(false);
		//doc.background_(Color.black);
		//doc.setStringColor(Color.green);
		//doc.bounds_(Rect(10, 500, 650, 800));
		//doc.font_(Font("Monaco",16));
		//doc.string_("

		var w, m, str = "
-----------  score modes  -----------
[	: melodic score
|	: percussive score
{	: concrete score (samples)

-----------  operators  -----------
->	: score assigned to an agent
>> 	: set effect
<< 	: delete effect
))	: increase amplitude
((	: decrease amplitude
tonic	: set the tonic

examples:  (@ sets repetition count)
name		score expression
agent1 -> piano[123456787654321]@1
agent2 -> |abcdefghijklmonp|@1
agent3 -> snow{9       }@1

	agent1 33 reverb
------------  arguments  ---------
!	: insert silence (!16 is silence for 16 beats)
@	: set the number of repetitions
+	: transpose in melodic mode, value can be multi-digit
-	: transpose in melodic mode,  value can be multi-digit
*	: expand the score in all modes,  value can be multi-digit
/	: contract the score in all modes,  value can be multi-digit
(..)	: duration divisors ( 1 - 9) durations can be scaled
	: (1244_8)  makes the durations 8 times larger, scaling value can be multi-digit
^..^	: control note accent (1 is quiet, 9 is loud)
<..>	: panning (1 is left, 9 right)
~ agent reps... ~	:sequence agents, repeating each one the specified time
			:MUST have spaces between tildes and argument
-----------  methods  -----------
doze 	: pause agent
perk 	: resume agent
nap	: pause agent for n seconds : n times
shake 	: randomise the score
swap 	: swap instruments in score
replace 	: replace instruments or notes in the score (in place)
>shift 	: right shift array n slot(s)
<shift 	: left shift array n slot(s)
invert	: invert the melody
expand	: expand the score with n nr. of silence(s)
revert	: revert the order
order	: organise in time
up	: to upper case (in rhythm mode)
down	: to lower case (in rhythm mode)
yoyo	: switch upper and lower case (in rhythm mode)

-----------  commands  -----------
tempo	: set tempo in bpm (accelerando op: tempo:time)
future	: set events in future (arg sec:times (4:4) or bars:times (4b:4))
group	: define a group
sequence	: define a sequence
scale	: set the scale for next agents
tuning 	: set the tuning for next agents
scalepush	: set the scale for all running agents
tuningpush	: set the tuning for all running agents
grid  	: draw a line every n spaces
remind	: get this document
instr 	: info on available instruments
tonality	: info on available scales and tunings
kill	: stop all sounds in window
snapshot	 -> 	: store current state as a snapshot (snapshot -> mySnap)
snapshot	 mySnap	: recall the snapshot
suicide 	: allow the language to kill itself sometime in the future
hotline	: if you change your mind wrgt the suicide
midiclients	: post the available midiclients
midiout	: set the port to the midiclient
store	: store the environmental setup of the session
load	: load the environment of a stored
savescore	: save the session
playscore	: play a saved session (exactly the same, unless you name a variation)
newrec 	: start a new recording and ignore all that has been typed and evaluated before
autocode	: autocode some agents and scores
";
		str.split(Char.nl);

		w = Window.new("ixi Lang Lingo", Rect(Window.screenBounds.width - 650, 100, 650, 350), scroll: false);
		w.front;
		// ListView(w, Rect(10, 10, 630, 330))
		// .items_(str.split(Char.nl))
		TextView(w, Rect(10, 10, 630, 330))
		.string_(str)
		.resize_(5);
	}


	getInstrumentsList {
		var m,t,u,e,f, w, r;
		r = Rect(Window.screenBounds.width - 650, Window.screenBounds.height - 500, 650, 500);
		w = Window.new("Xii Sounds, Scales, Tunings", r, scroll: false).front;

		// scales

		StaticText(w, Rect(10, 0, 119, 20)).string_("synths").align_(\center);
		m = ListView(w, Rect(10, 20, 119, 490)).resize_(4);
		m.items = ixiInstr.getProjectSynthesisSynthdefs.sort ++ ["-----------"] ++
		SynthDescLib.getLib(\xiilang).synthDescs.keys.asArray.sort[1..];

		StaticText(w, Rect(130, 0, 119, 20)).string_("effects").align_(\center);
		t = ListView(w, Rect(130, 20, 119, 490)).resize_(4);
		t.items = effectDict.keys.asArray.sort.collect({ | k | (k.asString) });

		// sample mappings - project specific
		StaticText(w, Rect(260, 0, 119, 20)).string_("sample mappings").align_(\center);
		u = ListView(w, Rect(260, 20, 119, 490)).resize_(4);
		u.items = instrDict.keys.asArray.sort.collect({ | k | (k.asString + instrDict[k]) });

		// effects - project specific, sort of
		StaticText(w, Rect(390, 0, 119, 20)).string_("scales").align_(\center);
		e = ListView(w, Rect(390, 20, 119, 490)).resize_(4);
		if( Scale.respondsTo(\all)) {
			e.items = Scale.all.parent.keys.asArray.sort.collect(_.asString);
		} {
			e.items = ScaleInfo.scales.keys.asArray.sort.collect(_.asString);
		};

		StaticText(w, Rect(520, 0, 119, 20)).string_("tunings").align_(\center);
		f = ListView(w, Rect(520, 20, 119, 490)).resize_(4);
		if( Tuning.respondsTo(\all)) {
			f.items = Tuning.all.parent.keys.asArray.sort.collect(_.asString);
		} {
			f.items = TuningInfo.tunings.keys.asArray.sort.collect(_.asString);
		};
	}

	agentPrefix {
		^docnum.asString;
//		^"";
	}

}

/*
eval
[ mode, 1 ]
------    ixi lang: Created Melodic Agent : test
[ (  ), ( 'instrument': string, 'mode': 1, 'notearr': [ 60, 62, 64 ], 'amp': 0.5,



a = s.waitForBoot { a = XiiLang(txt: true); };
a.agentDict['1test']
a.agentDict.keys
*/

XiiLangSingleton {
	classvar <>windowsList;

	*new{ |object|
		^super.new.initXiiLangSingleton(object);
	}

	initXiiLangSingleton { |object, windowsCount|
		if(windowsCount == 1, {
			windowsList = [object];
		}, {
			windowsList = windowsList.add(object);
		});
		windowsList.postln;
	}
}


+ String {

	// this is from wslib  /Lang/Improvements/extString-collect.sc
	collect { | function |
		// it changes functionality of some important methods (like .tr) so I have to include it here
		var result = "";
		this.do {|elem, i| result = result ++ function.value(elem, i); }
		^result;
	}

	ixilang { | window=nil, newagent=false |
		if(window.isNil, {
			Document.new.front.string_("XiiLang.new()").string.interpret;
			window = 0;
		});
		XiiLangSingleton.windowsList[window].opInterpreter(this);
		if(newagent, {
			{
			XiiLangSingleton.windowsList[window].doc.string = 			XiiLangSingleton.windowsList[window].doc.string ++ "\n" ++ this ++ "\n";
			}.defer(0.3);
		});
	}
}