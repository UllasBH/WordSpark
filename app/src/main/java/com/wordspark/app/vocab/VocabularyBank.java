package com.wordspark.app.vocab;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class VocabularyBank {

    private static final List<VocabCategory> CATEGORIES = new ArrayList<>();
    private static final String PREF_EXCLUDED_WORDS = "wordspark_excluded_vocab_words";

    static {
        CATEGORIES.add(new VocabCategory("academic", "Academic Vocabulary", Arrays.asList(
                "analysis", "approach", "assess", "assume", "authority", "available", "concept",
                "consistent", "context", "contract", "create", "data", "definition",
                "derived", "distribution", "economic", "environment", "established", "estimate",
                "evidence", "factor", "financial", "formula", "method", "theory", "structure",
                "function", "significant", "indicate", "variable", "research", "section",
                "process", "policy", "period", "individual", "export", "feature", "issue",
                "labour", "legal", "major", "percent", "principle", "proceed", "require",
                "response", "role", "sector", "source", "specific", "achieve", "acquisition",
                "administration", "affect", "appropriate", "aspect", "assistance", "category",
                "chapter", "commission", "community", "complex", "compute", "conclusion",
                "conduct", "consequence", "construction", "credit", "culture", "design",
                "element", "evaluation", "final", "focus", "impact", "injury", "institute",
                "investment", "item", "journal", "maintenance", "normal", "obtain",
                "participation", "perception", "positive", "potential", "previous", "primary",
                "purchase", "range", "region", "regulations", "relevant", "resident", "resource",
                "security", "select", "site", "strategy", "survey", "text", "tradition", "transfer"
        )));

        CATEGORIES.add(new VocabCategory("gre", "GRE Essentials", Arrays.asList(
                "abate", "adulate", "aesthetic", "ambivalent", "anomalous", "audacious", "benign",
                "capricious", "castigate", "chicanery", "cogent", "convoluted", "credulity", "cursory",
                "decorum", "didactic", "diffident", "disabuse", "discerning", "discordant", "disparate",
                "ebullient", "efficacious", "ephemeral", "equivocal", "erudite", "esoteric", "euphoric",
                "exacerbate", "exculpate", "exigent", "fallacious", "fatuous", "fawning", "fervid",
                "florid", "gainsay", "garrulous", "gregarious", "guile", "iconoclast", "idyll",
                "ignoble", "imbue", "immutable", "impassive", "impecunious", "imperious", "impertinent",
                "implacable", "importunate", "inchoate", "inimical", "innocuous", "insipid",
                "intractable", "inundate", "irascible", "itinerant", "laconic", "laud", "lavish",
                "lethargic", "loquacious", "lucid", "luminous", "magnanimity", "malinger", "malleable",
                "maverick", "mendacious", "mercurial", "meticulous", "mollify", "morose", "mundane",
                "munificent", "nadir", "neophyte", "obdurate", "obsequious", "occlude", "officious",
                "onerous", "opiate", "oscillate", "ostentatious", "paragon", "partisan", "pathological",
                "paucity", "pedantic", "penurious", "perfidious", "perfunctory", "permeate", "pervasive",
                "phlegmatic", "piety", "placate", "plasticity", "platitude", "plethora", "polemical",
                "pragmatic", "precipitate", "prevaricate", "probity", "prodigal", "profound", "proliferate",
                "propitiate", "propriety", "proscribe", "pungent", "quaff", "quandary", "quibble",
                "quiescent", "rancor", "recalcitrant", "recondite", "refractory", "reprobate", "rescind",
                "resilient", "respite", "reticent", "revere", "salubrious", "sanguine", "sapient",
                "sardonic", "sartorial", "satiate", "scintilla", "scrupulous", "secular", "sedulous",
                "sentient", "skeptical", "solicitous", "solvent", "soporific", "specious", "sporadic",
                "stigmatize", "stolid", "stringent", "sublime", "subversive", "superficial", "superfluous",
                "supplant", "surfeit", "sycophant", "taciturn", "temperance", "tenacious", "tenuous",
                "tirade", "torpor", "tractable", "transitory", "truculent", "turgid", "ubiquitous",
                "unctuous", "undermine", "unfeigned", "untenable", "vacillate", "venerate", "veracious",
                "verbose", "verisimilitude", "vex", "vigilant", "vindicate", "virulent", "viscous",
                "vociferous", "volatility", "voracious", "warranted", "welter", "whimsical", "zealot", "zenith"
        )));

        CATEGORIES.add(new VocabCategory("idioms", "Common Expressions", Arrays.asList(
                "diligent", "frugal", "vivid", "benevolent", "novice", "pragmatic", "astute",
                "eloquent", "tenacious", "candid", "gregarious", "meticulous", "cognizant",
                "luminous", "resilient", "ubiquitous", "mellifluous", "ephemeral", "serendipity",
                "sagacious", "zealot", "taciturn", "winsome", "vigilant", "lucid", "fervid",
                "pioneering", "intrepid", "audacious", "frank", "genuine", "authentic", "sincere",
                "earnest", "zealous", "passionate", "enthusiastic", "energetic", "dynamic",
                "versatile", "adaptable", "resourceful", "inventive", "creative", "imaginative",
                "visionary", "inspired", "brilliant", "talented", "gifted", "proficient",
                "competent", "capable", "accomplished", "expert", "masterful", "dexterous",
                "adroit", "deft", "clever", "smart", "intelligent", "wise", "judicious",
                "prudent", "sensible", "rational", "logical", "analytical", "observant",
                "perceptive", "discerning", "insightful", "penetrating", "profound", "thorough",
                "rigorous", "methodical", "systematic", "organized", "orderly", "neat", "tidy",
                "clean", "pure", "spotless", "immaculate", "flawless", "impeccable", "exemplary",
                "model", "ideal", "perfect", "supreme", "ultimate", "paramount", "preeminent",
                "outstanding", "exceptional", "extraordinary", "remarkable", "marvelous",
                "wonderful", "splendid", "magnificent", "majestic", "sublime", "glorious",
                "fabulous", "fantastic", "superb", "excellent", "stellar", "prime", "choice",
                "select", "preferred", "favorite", "beloved", "cherished", "treasured", "valued",
                "esteemed", "respected", "honored", "revered", "venerated", "praised", "acclaimed",
                "applauded", "celebrated", "renowned", "famous", "noted", "notable", "distinguished",
                "eminent", "prominent", "leading", "foremost", "principal", "main", "chief",
                "primary", "key", "central", "vital", "crucial", "essential", "fundamental",
                "basic", "core", "heart", "soul", "spirit", "mind", "thought", "idea", "notion",
                "concept", "theory", "principle", "law", "rule", "norm", "standard", "criterion",
                "measure", "gauge", "benchmark", "guide", "pattern", "template", "blueprint",
                "framework", "schema", "system", "structure", "design", "plan", "strategy",
                "tactic", "method", "approach", "technique", "procedure", "process", "operation",
                "action", "deed", "feat", "achievement", "accomplishment", "success", "victory",
                "triumph", "win", "gain", "profit", "benefit", "advantage", "asset", "resource",
                "wealth", "fortune", "abundance", "plenty", "prosperity", "affluence", "luxury",
                "comfort", "ease", "relief", "peace", "calm", "tranquility", "serenity", "harmony",
                "balance", "stability", "security", "safety", "protection", "defense", "shield",
                "guard", "bulwark", "anchor", "pillar", "support", "sustenance", "nourishment",
                "life", "breath", "pulse", "beat", "rhythm", "flow", "stream", "river", "ocean",
                "sea", "wave", "tide", "current", "wind", "storm", "breeze", "air", "sky", "sun",
                "star", "moon", "planet", "earth", "ground", "soil", "dust", "stone", "mountain",
                "hill", "valley", "forest", "woods", "tree", "plant", "flower", "leaf", "root",
                "seed", "fruit", "harvest", "spring", "summer", "autumn", "winter", "season",
                "time", "hour", "day", "week", "month", "year", "decade", "century", "millennium",
                "age", "era", "epoch", "date", "moment", "instant", "second", "minute"
        )));

        CATEGORIES.add(new VocabCategory("business", "Business & Workplace", Arrays.asList(
                "benchmark", "leverage", "stakeholder", "scalable", "turnover", "acquisition",
                "audit", "capital", "collateral", "consensus", "equity", "enterprise",
                "dividend", "portfolio", "asset", "liability", "revenue", "profit",
                "margin", "strategy", "budget", "valuation", "merger", "shareholder",
                "corporation", "subsidiary", "liquidity", "solvency", "depreciation",
                "amortization", "tariff", "subsidy", "inflation", "recession", "deflation",
                "monopoly", "venture", "startup", "franchise", "conglomerate", "partnership",
                "headquarters", "branch", "office", "department", "division", "employee",
                "employer", "manager", "director", "executive", "officer", "president",
                "supervisor", "coordinator", "analyst", "consultant", "contractor", "supplier",
                "vendor", "client", "customer", "consumer", "buyer", "seller", "investor",
                "broker", "trader", "banker", "auditor", "accountant", "actuary", "economist",
                "marketer", "advertiser", "salesperson", "negotiator", "mediator", "arbitrator",
                "receptionist", "secretary", "assistant", "clerk", "intern", "apprentice",
                "colleague", "coworker", "peer", "mentor", "protege", "successor", "predecessor",
                "rival", "competitor", "adversary", "partner", "associate", "team", "staff",
                "personnel", "workforce", "labor", "talent", "recruitment", "hiring", "onboarding",
                "training", "development", "promotion", "demotion", "transfer", "resignation",
                "retirement", "dismissal", "layoff", "strike", "union", "contract", "agreement",
                "negotiation", "compromise", "concession", "deal", "transaction", "payment",
                "invoice", "receipt", "bill", "check", "draft", "credit", "debit", "loan",
                "mortgage", "debt", "interest", "tax", "duty", "excise", "levy", "assessment",
                "appraisal", "inspection", "review", "report", "statement", "balance", "ledger",
                "journal", "record", "file", "document", "policy", "procedure", "guideline",
                "standard", "protocol", "compliance", "regulation", "governance", "ethics",
                "integrity", "transparency", "accountability"
        )));

        CATEGORIES.add(new VocabCategory("tech", "Technology & Computing", Arrays.asList(
                "algorithm", "bit", "cloud", "compile", "cookie", "database",
                "encryption", "firewall", "hardware", "network", "packet", "protocol",
                "software", "server", "router", "binary", "pixel", "monitor",
                "browser", "digital", "backup", "code", "storage", "system",
                "processor", "memory", "cache", "bandwidth", "latency", "throughput",
                "interface", "application", "platform", "framework", "library", "module",
                "package", "dependency", "repository", "version", "branch", "merge",
                "commit", "push", "pull", "clone", "fork", "issue", "bug", "debug",
                "patch", "update", "upgrade", "install", "setup", "configure", "execute",
                "runtime", "syntax", "token", "variable", "constant", "function", "method",
                "class", "object", "instance", "inheritance", "polymorphism", "encapsulation",
                "abstraction", "pointer", "reference", "array", "string", "integer", "boolean",
                "float", "double", "null", "exception", "error", "warning", "log", "trace",
                "console", "terminal", "command", "script", "shell", "kernel", "driver",
                "firmware", "bios", "boot", "disk", "drive", "partition", "format",
                "file", "folder", "directory", "path", "url", "uri", "domain", "host",
                "port", "socket", "tcp", "udp", "ip", "dns", "http", "https", "ssl",
                "tls", "ssh", "ftp", "smtp", "imap", "api", "json", "xml", "yaml",
                "html", "css", "javascript", "python", "java", "kotlin", "swift", "rust",
                "golang", "sql", "nosql", "mongodb", "redis", "postgresql", "mysql",
                "sqlite", "docker", "kubernetes", "container", "virtualization", "hypervisor",
                "aws", "azure", "gcp", "serverless", "lambda", "microservice", "monolith",
                "devops", "ci", "cd", "pipeline", "automation", "testing", "unit", "integration",
                "regression", "benchmark", "profiling", "scaling", "load", "balancer",
                "proxy", "gateway", "hashing", "tokenization", "authentication", "authorization",
                "permission", "role", "session", "oauth", "jwt", "certificate", "key",
                "public", "private", "symmetric", "asymmetric", "cipher", "entropy",
                "malware", "virus", "worm", "trojan", "ransomware", "spyware", "adware",
                "phishing", "spoofing", "sniffing", "botnet", "exploit", "vulnerability",
                "payload", "sandbox", "honeypot"
        )));

        CATEGORIES.add(new VocabCategory("advanced", "Advanced English", Arrays.asList(
                "perspicacious", "pusillanimous", "recalcitrant", "recondite", "sagacious",
                "salubrious", "sanguine", "solipsism", "supercilious", "taciturn", "temerity",
                "truculent", "vacillate", "verisimilitude", "vicissitude", "vitriolic", "vociferous",
                "winsome", "zenith", "aberration", "abstemious", "acumen", "adroit", "alacrity",
                "anachronism", "analogy", "anodyne", "antipathy", "apathy", "apocryphal", "apogee",
                "apostate", "apposite", "apprise", "approbation", "arabesque", "arcane", "archaic",
                "ardent", "arrogate", "ascetic", "asperity", "assiduous", "attenuate", "audacious",
                "austere", "autonomous", "aver", "avocation", "avuncular", "axiomatic", "bacchanalian",
                "banal", "beatific", "beleaguer", "belie", "bellicose", "beneficent", "bereft",
                "bifurcate", "bilious", "blanch", "blandishment", "boorish", "bourgeois", "brash",
                "brazen", "bucolic", "burnish", "buttress", "cacophony", "cadge", "cajole", "callow",
                "calumny", "candor", "canon", "capricious", "cardinal", "carnal", "carping",
                "cartography", "cascade", "castigate", "catalyst", "categorical", "caucus", "caustic",
                "cavalier", "celerity", "censorious", "censure", "centrifugal", "centripetal",
                "ceremonious", "cession", "chagrin", "charlatan", "chary", "chasm", "chicanery",
                "chivalrous", "choleric", "churlish", "circuitous", "circumlocution", "circumspect",
                "circumvent", "clairvoyant", "clamor", "clandestine", "cleave", "clement", "clique",
                "cloying", "coagulate", "coalesce", "codicil", "cogent", "cognate", "cognizant",
                "cohesive", "collaborate", "collate", "colloquial", "collusion", "colossal",
                "combustible", "commensurate", "commiserate", "commodious", "compendium", "complacent",
                "compliant", "complicity", "component", "comport", "compound", "comprehend",
                "comprehensive", "compress", "compunction", "compute", "concatenate", "concave",
                "concede", "conceit", "concentric", "conception", "concerted", "conciliate",
                "concise", "conclave", "concoct", "concomitant", "concord", "concur", "condescend",
                "condign", "condone", "conducive", "confidant", "confiscate", "confluence",
                "conform", "confound", "congeal", "congenial", "congenital", "congregate",
                "congruent", "conifer", "conjecture", "conjugal", "connive", "connoisseur",
                "consanguinity", "consecrate", "consensus", "consequential", "conservatory",
                "consign", "consistent", "consolation", "consolidate", "consonant", "consort",
                "conspicuous", "conspiratorial", "consternation", "constituent", "constrain",
                "constructive", "construe", "consummate", "contagious", "contaminate", "contemn",
                "contemplate", "contemptuous", "contend", "contentious", "contiguous", "continent",
                "contingent", "contortion", "contraband", "contradict", "contravention", "contrite",
                "contrivance", "controvert", "contumacious", "conundrum", "convene", "converge",
                "conversant", "converse", "convert", "convex", "convey", "conviction", "convivial",
                "convoke", "convoluted", "copious", "cordial", "cornucopia", "corollary", "corporeal",
                "corpulent", "correlate", "corroborate", "coruscate", "coterie", "countenance",
                "countermand", "counterpart", "coup", "courageous", "covert", "covet", "cower",
                "coy", "crass", "craven", "credence", "credulous", "creed", "crepuscular", "criterium",
                "cronyism", "cross", "crucial", "crux", "cryptic", "culpable", "culminate",
                "cumbrous", "cumulative", "cupidity", "curator", "curmudgeon", "cursive", "cursory",
                "curt", "curtail", "cynical", "daunt", "dank", "dappled", "dart", "dauntless",
                "earthy", "ebbing", "ebullient", "eccentric", "ecclesiastic", "eclectic", "eclipse",
                "ecstasy", "edify", "educe", "efface", "effectual", "effeminate", "effervescent",
                "efficacious", "effigy", "effluvium", "effrontery", "effusive", "egoist", "egregious",
                "ejaculate", "elaborate", "elastic", "elegy", "elicit", "elixir", "ellipsis",
                "eloquent", "elucidate", "elusive", "emaciated", "emanate", "emancipate", "embargo",
                "embark", "embed", "embellish", "embezzle", "emblazon", "embodiment", "emboss",
                "embrace", "embroil", "embryonic", "emend", "emerald", "emerge", "emetic",
                "emigrant", "eminent", "emissary", "emit", "emollient", "emote", "empirical",
                "empiric", "empower", "enamor", "encamp", "encomium", "encompass", "encore",
                "encroach", "encumber", "endemic", "endorse", "endow", "endurance", "energize",
                "enervate", "enfranchise", "engender", "engross", "engulf", "enhance", "enigma",
                "enjoin", "enlighten", "enlist", "enliven", "enmity", "ennui", "enormity",
                "enrapture", "ensconce", "ensemble", "ensue", "entail", "entangle", "enterprise",
                "enthrall", "enthusiasm", "entice", "entirety", "entity", "entomology", "entourage",
                "entreat", "entrench", "entrepreneur", "enumerate", "enunciate", "environ",
                "ephemeral", "epic", "epicure", "epigram", "epilogue", "epiphany", "episode",
                "epitaph", "epithet", "epitome", "epoch", "equable", "equanimity", "equate",
                "equinox", "equity", "equivocal", "eradicate", "errant", "erratic", "erroneous",
                "erudite", "eschew", "esoteric", "espouse", "esprit", "essay", "esteem", "esthetic",
                "estimable", "estrange", "etch", "ethereal", "ethics", "ethnic", "etiology",
                "etymology", "eulogy", "euphemism", "euphony", "euphoria", "evacuate", "evanescent",
                "evasive", "evenhanded", "evict", "evince", "eviscerate", "evocative", "exacerbate",
                "exact", "exacting", "exalt", "exasperate", "excavate", "excerpt", "excise",
                "excite", "exclaim", "exclude", "excoriate", "excrement", "excruciating", "excuse",
                "exculpate", "execrable", "execute", "exemplar", "exemplify", "exempt", "exercise",
                "exert", "exhalation", "exhaust", "exhaustive", "exhort", "exhume", "exigency",
                "exigent", "exile", "existent", "existential", "exodus", "exonerate", "exorbitant",
                "exorcise", "exotic", "expand", "expanse", "expansive", "expatiate", "expect",
                "expedient", "expedite", "expel", "expend", "expiate", "explicate", "explicit",
                "explode", "exponent", "export", "expose", "expostulate", "expound", "express",
                "expressive", "expulsion", "expunge", "expurgate", "exquisite", "extant",
                "extemporaneous", "extend", "extensive", "extent", "extenuate", "exterior",
                "exterminate", "external", "extinct", "extinguish", "extirpate", "extol", "extort",
                "extract", "extradite", "extraneous", "extraordinary", "extrapolate", "extravagant",
                "extremity", "extricate", "extrinsic", "extrovert", "exuberant", "exude", "exult",
                "fabricate", "facade", "facet", "facetious", "facile", "facilitate", "facsimile",
                "faction", "factitious", "factotum", "faculty"
        )));
    }

    public static List<VocabCategory> getCategories() {
        return CATEGORIES;
    }

    public static VocabCategory getCategory(String id) {
        if (id == null) return null;
        for (VocabCategory cat : CATEGORIES) {
            if (cat.getId().equals(id)) return cat;
        }
        return null;
    }

    public static void excludeWord(Context context, String word) {
        if (context == null || word == null) return;
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_EXCLUDED_WORDS, Context.MODE_PRIVATE);
        Set<String> excluded = new HashSet<>(prefs.getStringSet("excluded", new HashSet<>()));
        excluded.add(word.trim().toLowerCase(Locale.ROOT));
        prefs.edit().putStringSet("excluded", excluded).apply();
    }

    public static Set<String> getExcludedWords(Context context) {
        if (context == null) return new HashSet<>();
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_EXCLUDED_WORDS, Context.MODE_PRIVATE);
        return prefs.getStringSet("excluded", new HashSet<>());
    }

    public static List<String> getDailyWordsForCategory(Context context, VocabCategory category) {
        Set<String> excluded = getExcludedWords(context);
        List<String> pool = new ArrayList<>();
        for (String w : category.getAllWords()) {
            if (!excluded.contains(w.trim().toLowerCase(Locale.ROOT))) {
                pool.add(w);
            }
        }
        if (pool.size() <= 25) {
            return pool;
        }

        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);
        long seed = (long) year * 1000 + dayOfYear + category.getId().hashCode();

        Random random = new Random(seed);
        List<String> mutablePool = new ArrayList<>(pool);
        Collections.shuffle(mutablePool, random);

        int count = Math.min(25, mutablePool.size());
        return mutablePool.subList(0, count);
    }
}
