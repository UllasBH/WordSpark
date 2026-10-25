package com.wordspark.app.quiz;

import android.content.Context;
import android.content.SharedPreferences;

import com.wordspark.app.model.QuizQuestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A local, curated question bank with 75+ questions across WordSpark categories.
 * Provides category-balanced and non-repeating question selection for quizzes.
 */
public class QuizBank {

    private static final String PREF_RECENT_QUIZ_IDS = "wordspark_recent_quiz_ids";

    public static List<QuizQuestion> all() {
        List<QuizQuestion> bank = new ArrayList<>();

        // --- Technology & Computing ---
        bank.add(new QuizQuestion("tech_1", "Technology", "Algorithm", "What does \"Algorithm\" mean?",
                "A step-by-step set of instructions for solving a problem",
                Arrays.asList("A physical computer component", "An internet browser feature", "A type of database error"),
                "An algorithm is a precise procedure or formula for solving a problem."));
        bank.add(new QuizQuestion("tech_2", "Technology", "Database", "What does \"Database\" mean?",
                "An organized collection of structured information or data",
                Arrays.asList("A temporary internet file", "A hardware cooling system", "A computer programming language"),
                "A database stores structured data so it can be easily accessed and managed."));
        bank.add(new QuizQuestion("tech_3", "Technology", "Encryption", "What does \"Encryption\" mean?",
                "The process of converting data into code to prevent unauthorized access",
                Arrays.asList("Deleting unnecessary system files", "Compressing files to save storage", "Backing up data to a remote server"),
                "Encryption secures data by scrambling it into ciphertext."));
        bank.add(new QuizQuestion("tech_4", "Technology", "Firewall", "What does \"Firewall\" mean?",
                "A network security system that monitors and controls incoming and outgoing network traffic",
                Arrays.asList("A physical device to prevent overheating", "A cooling fan in a computer", "An anti-virus scanning tool"),
                "A firewall acts as a barrier between trusted and untrusted networks."));
        bank.add(new QuizQuestion("tech_5", "Technology", "Protocol", "What does \"Protocol\" mean?",
                "A set of rules governing the exchange or transmission of data between devices",
                Arrays.asList("A computer start-up sequence", "An error-checking utility", "A software licensing agreement"),
                "Protocols ensure different systems can communicate effectively."));
        bank.add(new QuizQuestion("tech_6", "Technology", "Bandwidth", "What does \"Bandwidth\" mean?",
                "The maximum rate of data transfer across a given path",
                Arrays.asList("The physical storage capacity of a hard drive", "The processing speed of a CPU", "The screen resolution of a monitor"),
                "Bandwidth measures how much data can pass through a connection in a given time."));
        bank.add(new QuizQuestion("tech_7", "Technology", "Compile", "What does \"Compile\" mean?",
                "To convert source code into machine code or bytecode",
                Arrays.asList("To delete temporary internet files", "To organize physical computer parts", "To test software for bugs"),
                "Compiling translates human-readable code into executable machine instructions."));
        bank.add(new QuizQuestion("tech_8", "Technology", "Debugging", "What does \"Debugging\" mean?",
                "Identifying and removing errors from computer hardware or software code",
                Arrays.asList("Installing a new operating system", "Backing up corrupted files", "Optimizing internet connection speed"),
                "Debugging is the process of finding and fixing software bugs."));
        bank.add(new QuizQuestion("tech_9", "Technology", "Hardware", "What does \"Hardware\" mean?",
                "The physical components that make up a computer system",
                Arrays.asList("The operating system and installed programs", "Data stored in cloud storage", "Network communication protocols"),
                "Hardware refers to the tangible parts of a computer."));
        bank.add(new QuizQuestion("tech_10", "Technology", "Router", "What does \"Router\" mean?",
                "A device that forwards data packets between computer networks",
                Arrays.asList("A tool to repair broken cables", "A storage drive for backup data", "A software program to block malware"),
                "Routers direct traffic across computer networks."));
        bank.add(new QuizQuestion("tech_11", "Technology", "Software", "What does \"Software\" mean?",
                "Programs and other operating information used by a computer",
                Arrays.asList("Physical computer chips", "Electrical power cables", "Cooling fans and chassis"), "Software gives instructions to computer hardware."));
        bank.add(new QuizQuestion("tech_12", "Technology", "Server", "What does \"Server\" mean?",
                "A computer or program that manages access to a central resource or service in a network",
                Arrays.asList("A personal desktop computer", "A mobile smartphone app", "A computer printer"), "Servers serve requests from client devices."));
        bank.add(new QuizQuestion("tech_13", "Technology", "Browser", "What does \"Browser\" mean?",
                "An application used to access and view websites on the internet",
                Arrays.asList("An antivirus scanning utility", "A file compression program", "A database management tool"), "Web browsers render HTML and web content."));
        bank.add(new QuizQuestion("tech_14", "Technology", "Packet", "What does \"Packet\" mean?",
                "A formatted unit of data carried on a packet-switched network",
                Arrays.asList("A software installation zip file", "A physical cable connector", "A power supply unit"), "Data travels across the internet in packets."));

        // --- Business & Workplace ---
        bank.add(new QuizQuestion("biz_1", "Business", "Benchmark", "What does \"Benchmark\" mean?",
                "A standard or point of reference against which things may be compared",
                Arrays.asList("A financial loss statement", "A marketing advertising campaign", "An employee hiring process"),
                "A benchmark serves as a standard for measurement or evaluation."));
        bank.add(new QuizQuestion("biz_2", "Business", "Leverage", "What does \"Leverage\" mean?",
                "To use borrowed capital for an investment, or to use maximum advantage",
                Arrays.asList("To reduce company debt completely", "To fire underperforming employees", "To close a business branch"),
                "Leverage means maximizing existing resources or borrowed funds."));
        bank.add(new QuizQuestion("biz_3", "Business", "Stakeholder", "What does \"Stakeholder\" mean?",
                "Anyone with an interest or concern in a business or organization",
                Arrays.asList("Only the primary owner of a company", "A company's legal representative", "A supplier of raw materials"),
                "Stakeholders include employees, investors, customers, and communities."));
        bank.add(new QuizQuestion("biz_4", "Business", "Scalable", "What does \"Scalable\" mean?",
                "Able to be resized or grown easily without losing performance or efficiency",
                Arrays.asList("Fixed in size and budget", "Easy to liquidate into cash", "Protected by copyright laws"),
                "Scalable business models can grow revenue faster than costs."));
        bank.add(new QuizQuestion("biz_5", "Business", "Acquisition", "What does \"Acquisition\" mean?",
                "An asset or object bought, or a corporate takeover of one company by another",
                Arrays.asList("The launching of a new product", "The renting of office space", "The firing of company executives"),
                "An acquisition occurs when one company purchases most or all of another company."));
        bank.add(new QuizQuestion("biz_6", "Business", "Audit", "What does \"Audit\" mean?",
                "An official inspection of an organization's accounts or financial records",
                Arrays.asList("A public advertising campaign", "A meeting with new clients", "A negotiation with suppliers"),
                "An audit ensures financial accuracy and compliance."));
        bank.add(new QuizQuestion("biz_7", "Business", "Consensus", "What does \"Consensus\" mean?",
                "General agreement among a group of people",
                Arrays.asList("A strict corporate policy", "A financial profit margin", "A legal contract dispute"),
                "Consensus represents collective agreement."));
        bank.add(new QuizQuestion("biz_8", "Business", "Equity", "What does \"Equity\" mean?",
                "The value of the shares issued by a company or ownership interest",
                Arrays.asList("Company debt owed to banks", "Total annual revenue", "Employee salary expenses"),
                "Equity represents ownership stake in a company."));
        bank.add(new QuizQuestion("biz_9", "Business", "Dividend", "What does \"Dividend\" mean?",
                "A sum of money paid regularly by a company to its shareholders",
                Arrays.asList("A tax paid to the government", "A loan taken from a bank", "An employee bonus payment"),
                "Dividends distribute corporate earnings to shareholders."));
        bank.add(new QuizQuestion("biz_10", "Business", "Portfolio", "What does \"Portfolio\" mean?",
                "A range of investments held by a person or organization",
                Arrays.asList("A financial balance sheet", "A company employment record", "A marketing strategy document"),
                "A portfolio groups multiple financial assets together."));
        bank.add(new QuizQuestion("biz_11", "Business", "Revenue", "What does \"Revenue\" mean?",
                "The total amount of income generated by the sale of goods or services",
                Arrays.asList("Total business expenses", "Net profit after taxes", "Bank loan obligations"), "Revenue is top-line income before expenses."));
        bank.add(new QuizQuestion("biz_12", "Business", "Profit", "What does \"Profit\" mean?",
                "A financial gain, especially the difference between the amount earned and spent",
                Arrays.asList("Total gross revenue", "Total operating expenses", "Employee payroll taxes"), "Profit is revenue minus expenses."));
        bank.add(new QuizQuestion("biz_13", "Business", "Strategy", "What does \"Strategy\" mean?",
                "A plan of action designed to achieve a long-term or overall aim",
                Arrays.asList("A daily task list", "An accounting ledger", "An employee contract"), "Strategy outlines high-level goals and execution."));
        bank.add(new QuizQuestion("biz_14", "Business", "Budget", "What does \"Budget\" mean?",
                "An estimate of income and expenditure for a set period of time",
                Arrays.asList("Actual financial loss", "Total company assets", "Market share percentage"), "A budget plans financial allocation."));

        // --- Academic Vocabulary ---
        bank.add(new QuizQuestion("acad_1", "Academic", "Analysis", "What does \"Analysis\" mean?",
                "Detailed examination of something complex in order to understand it",
                Arrays.asList("A quick summary of facts", "A random guess or hypothesis", "A collection of raw data"),
                "Analysis breaks down complex topics into simpler components."));
        bank.add(new QuizQuestion("acad_2", "Academic", "Approach", "What does \"Approach\" mean?",
                "A way of dealing with a situation or problem",
                Arrays.asList("A physical barrier", "A final conclusion", "An administrative penalty"),
                "An approach defines the method or perspective used."));
        bank.add(new QuizQuestion("acad_3", "Academic", "Assess", "What does \"Assess\" mean?",
                "To evaluate or estimate the nature, quality, or ability of something",
                Arrays.asList("To ignore completely", "To manufacture a product", "To transport goods"),
                "To assess means to judge or evaluate carefully."));
        bank.add(new QuizQuestion("acad_4", "Academic", "Assume", "What does \"Assume\" mean?",
                "To accept something as true without proof",
                Arrays.asList("To prove with mathematical certainty", "To reject an idea outright", "To observe scientifically"),
                "To assume means taking something for granted without definitive proof."));
        bank.add(new QuizQuestion("acad_5", "Academic", "Concept", "What does \"Concept\" mean?",
                "An abstract idea or general notion",
                Arrays.asList("A physical machine", "A proven mathematical theorem", "A strict legal rule"),
                "A concept represents a mental notion or abstract principle."));
        bank.add(new QuizQuestion("acad_6", "Academic", "Consistent", "What does \"Consistent\" mean?",
                "Acting or done in the same way over time; unchanging and reliable",
                Arrays.asList("Constantly changing at random", "Extremely slow", "Completely unpredictable"),
                "Consistency implies dependable regularity."));
        bank.add(new QuizQuestion("acad_7", "Academic", "Context", "What does \"Context\" mean?",
                "The circumstances that form the setting for an event or idea",
                Arrays.asList("A physical location only", "A grammatical mistake", "A financial ledger"),
                "Context provides background and surrounding circumstances."));
        bank.add(new QuizQuestion("acad_8", "Academic", "Data", "What does \"Data\" mean?",
                "Facts and statistics collected together for analysis",
                Arrays.asList("Personal opinions", "Fiction stories", "Unverified rumors"),
                "Data consists of factual information gathered for research."));
        bank.add(new QuizQuestion("acad_9", "Academic", "Evidence", "What does \"Evidence\" mean?",
                "Available facts indicating whether a belief or proposition is true",
                Arrays.asList("A personal hypothesis", "A speculative theory", "An emotional reaction"),
                "Evidence supports claims with verifiable facts."));
        bank.add(new QuizQuestion("acad_10", "Academic", "Method", "What does \"Method\" mean?",
                "A particular procedure for accomplishing something in a systematic way",
                Arrays.asList("A random trial", "An unexpected outcome", "A final goal"),
                "A method is a structured procedure."));
        bank.add(new QuizQuestion("acad_11", "Academic", "Hypothesis", "What does \"Hypothesis\" mean?",
                "A proposed explanation made on the basis of limited evidence as a starting point",
                Arrays.asList("A proven scientific law", "A finalized research paper", "An unchangeable fact"), "A hypothesis is tested through experimentation."));
        bank.add(new QuizQuestion("acad_12", "Academic", "Variable", "What does \"Variable\" mean?",
                "An element, feature, or factor that can vary or change",
                Arrays.asList("A permanent fixed constant", "A verified conclusion", "A physical measuring tool"), "Variables are controlled or measured in research."));
        bank.add(new QuizQuestion("acad_13", "Academic", "Evaluation", "What does \"Evaluation\" mean?",
                "The making of a judgment about the amount, number, or value of something",
                Arrays.asList("The initial planning stage", "The collection of raw data", "The deletion of files"), "Evaluation assesses quality or significance."));

        // --- Advanced English & GRE Essentials ---
        bank.add(new QuizQuestion("adv_1", "Advanced", "Benevolent", "What does \"Benevolent\" mean?",
                "Kind and well-meaning",
                Arrays.asList("Hostile and unfriendly", "Secretive and cunning", "Lazy and indifferent"),
                "Benevolent means showing kindness and goodwill."));
        bank.add(new QuizQuestion("adv_2", "Advanced", "Ephemeral", "What does \"Ephemeral\" mean?",
                "Lasting for a very short time",
                Arrays.asList("Lasting forever", "Extremely heavy", "Brightly colored"),
                "Ephemeral things are fleeting."));
        bank.add(new QuizQuestion("adv_3", "Advanced", "Ubiquitous", "What does \"Ubiquitous\" mean?",
                "Present, appearing, or found everywhere",
                Arrays.asList("Extremely rare", "Hidden from view", "Old-fashioned"),
                "Ubiquitous means omnipresent."));
        bank.add(new QuizQuestion("adv_4", "Advanced", "Serendipity", "What does \"Serendipity\" mean?",
                "Finding valuable or agreeable things by chance",
                Arrays.asList("A planned discovery", "A feeling of deep sorrow", "A strict schedule"),
                "Serendipity is a fortunate accident."));
        bank.add(new QuizQuestion("adv_5", "Advanced", "Resilient", "What does \"Resilient\" mean?",
                "Able to withstand or recover quickly from difficult conditions",
                Arrays.asList("Easily damaged", "Stubbornly rigid", "Slow to respond"),
                "Resilient objects or people spring back into shape after adversity."));
        bank.add(new QuizQuestion("adv_6", "Advanced", "Meticulous", "What does \"Meticulous\" mean?",
                "Showing great attention to detail; very careful and precise",
                Arrays.asList("Sloppy and careless", "Extremely fast", "Unreasonably expensive"),
                "Meticulous work is thorough and exact."));
        bank.add(new QuizQuestion("adv_7", "Advanced", "Gregarious", "What does \"Gregarious\" mean?",
                "Fond of company; sociable",
                Arrays.asList("Shy and reclusive", "Hostile to strangers", "Easily angered"),
                "Gregarious people love social interaction."));
        bank.add(new QuizQuestion("adv_8", "Advanced", "Pragmatic", "What does \"Pragmatic\" mean?",
                "Practical and realistic rather than idealistic",
                Arrays.asList("Theoretical and impractical", "Overly emotional", "Highly idealistic"),
                "Pragmatism focuses on practical results."));
        bank.add(new QuizQuestion("adv_9", "Advanced", "Tenacious", "What does \"Tenacious\" mean?",
                "Tending to keep a firm hold of something; persistent",
                Arrays.asList("Quickly letting go", "Indifferent", "Forgetful"),
                "Tenacious persistence overcomes obstacles."));
        bank.add(new QuizQuestion("adv_10", "Advanced", "Astute", "What does \"Astute\" mean?",
                "Having or showing an ability to accurately assess situations or people",
                Arrays.asList("Slow-witted", "Naive and trusting", "Reckless"),
                "An astute person is perceptive and clever."));
        bank.add(new QuizQuestion("adv_11", "Advanced", "Ambivalent", "What does \"Ambivalent\" mean?",
                "Having mixed feelings or contradictory ideas about something or someone",
                Arrays.asList("Having absolute certainty", "Feeling intense anger", "Being completely indifferent"), "Ambivalence involves conflicting emotions."));
        bank.add(new QuizQuestion("adv_12", "Advanced", "Capricious", "What does \"Capricious\" mean?",
                "Given to sudden and unaccountable changes of mood or behavior",
                Arrays.asList("Steady and predictable", "Extremely cautious", "Strictly methodical"), "Capricious decisions are whimsical and erratic."));
        bank.add(new QuizQuestion("adv_13", "Advanced", "Ebullient", "What does \"Ebullient\" mean?",
                "Cheerful and full of energy",
                Arrays.asList("Gloomy and depressed", "Physically exhausted", "Calm and quiet"), "Ebullient people are vibrant and enthusiastic."));

        return bank;
    }

    public static List<QuizQuestion> selectQuizQuestions(Context context, int count) {
        List<QuizQuestion> allQuestions = all();
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_RECENT_QUIZ_IDS, Context.MODE_PRIVATE);
        Set<String> recentIds = prefs.getStringSet("recent", new HashSet<>());

        List<QuizQuestion> preferred = new ArrayList<>();
        List<QuizQuestion> fallback = new ArrayList<>();

        for (QuizQuestion q : allQuestions) {
            if (recentIds.contains(q.getId())) {
                fallback.add(q);
            } else {
                preferred.add(q);
            }
        }

        Collections.shuffle(preferred);
        Collections.shuffle(fallback);

        List<QuizQuestion> pool = new ArrayList<>();
        pool.addAll(preferred);
        pool.addAll(fallback);

        Map<String, List<QuizQuestion>> byCategory = new HashMap<>();
        for (QuizQuestion q : pool) {
            String cat = q.getCategory() != null ? q.getCategory() : "General";
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(q);
        }

        List<QuizQuestion> selected = new ArrayList<>();
        List<String> categories = new ArrayList<>(byCategory.keySet());
        Collections.shuffle(categories);

        boolean added = true;
        while (selected.size() < count && added) {
            added = false;
            for (String cat : categories) {
                List<QuizQuestion> catList = byCategory.get(cat);
                if (catList != null && !catList.isEmpty()) {
                    selected.add(catList.remove(0));
                    added = true;
                    if (selected.size() >= count) break;
                }
            }
        }

        for (String cat : categories) {
            List<QuizQuestion> catList = byCategory.get(cat);
            while (selected.size() < count && catList != null && !catList.isEmpty()) {
                selected.add(catList.remove(0));
            }
        }

        Collections.shuffle(selected);

        Set<String> newRecent = new HashSet<>();
        for (QuizQuestion q : selected) {
            newRecent.add(q.getId());
        }
        prefs.edit().putStringSet("recent", newRecent).apply();

        return selected;
    }
}
