package com.ArguMind.ArguMind.config;

import com.ArguMind.ArguMind.model.DebateTopic;
import com.ArguMind.ArguMind.model.FallacyGuide;
import com.ArguMind.ArguMind.repository.DebateTopicRepository;
import com.ArguMind.ArguMind.repository.FallacyGuideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DebateTopicRepository debateTopicRepository;
    private final FallacyGuideRepository fallacyGuideRepository;

    @Override
    public void run(String... args) {
        seedTopics();
        seedFallacyGuides();
    }

    private void seedTopics() {
        String[][] topics = {
                {"Inteligența Artificială este periculoasă pentru umanitate", "Tehnologie",
                        "Artificial Intelligence is dangerous for humanity", "Technology"},
                {"Educația online este superioară celei tradiționale", "Educație",
                        "Online education is superior to traditional education", "Education"},
                {"Rețelele sociale au un impact net negativ asupra societății", "Societate",
                        "Social networks have a net negative impact on society", "Society"},
                {"Guvernele ar trebui să reglementeze strict inteligența artificială", "Tehnologie",
                        "Governments should strictly regulate artificial intelligence", "Technology"},
                {"Învățământul universitar ar trebui să fie gratuit pentru toți", "Educație",
                        "University education should be free for everyone", "Education"},
                {"Eutanazia ar trebui legalizată în toate cazurile terminale", "Etică",
                        "Euthanasia should be legalized in all terminal cases", "Ethics"},
                {"Democrația directă este superioară democrației reprezentative", "Politică",
                        "Direct democracy is superior to representative democracy", "Politics"},
                {"Energia nucleară este soluția principală pentru criza climatică", "Mediu",
                        "Nuclear energy is the main solution to the climate crisis", "Environment"},
                {"Intervenția umană în natură este justificată pentru progres", "Mediu",
                        "Human intervention in nature is justified for progress", "Environment"},
                {"Cenzura online protejează societatea mai mult decât o limitează", "Societate",
                        "Online censorship protects society more than it limits it", "Society"},
                {"Munca remote este mai productivă decât cea la birou", "Business",
                        "Remote work is more productive than office work", "Business"},
                {"Antreprenoriatul ar trebui predat obligatoriu în liceu", "Educație",
                        "Entrepreneurship should be mandatory in high school", "Education"},
                {"Imigrația masivă aduce beneficii nete economiei", "Societate",
                        "Mass immigration brings net benefits to the economy", "Society"},
                {"Religia are un rol pozitiv în societatea modernă", "Societate",
                        "Religion has a positive role in modern society", "Society"},
                {"Sportul profesionist promovează valori negative tinerilor", "Sport",
                        "Professional sports promote negative values to youth", "Sports"},
                {"Universitățile ar trebui să elimine examenele standardizate", "Educație",
                        "Universities should eliminate standardized exams", "Education"},
                {"Capitalismul este cel mai eficient sistem economic", "Economie",
                        "Capitalism is the most efficient economic system", "Economy"},
                {"Statul ar trebui să controleze prețurile la utilități", "Economie",
                        "The state should control utility prices", "Economy"},
                {"Vaccinarea obligatorie este necesară în societate", "Sănătate",
                        "Mandatory vaccination is necessary in society", "Health"},
                {"Dieta plant-based ar trebui promovată oficial de stat", "Sănătate",
                        "A plant-based diet should be officially promoted by the state", "Health"},
                {"Jocurile video dezvoltă abilități cognitive utile", "Tehnologie",
                        "Video games develop useful cognitive skills", "Technology"},
                {"Inteligența emoțională este mai importantă decât IQ-ul", "Psihologie",
                        "Emotional intelligence is more important than IQ", "Psychology"},
                {"Părinții ar trebui să limiteze ecranele copiilor sub 12 ani", "Familie",
                        "Parents should limit children's screen time under age 12", "Family"},
                {"Criza climatică necesită reduceri drastice imediate ale consumului", "Mediu",
                        "The climate crisis requires immediate drastic cuts in consumption", "Environment"},
                {"Armele de foc ar trebui interzise civililor", "Politică",
                        "Firearms should be banned for civilians", "Politics"},
                {"Inteligența artificială va înlocui majoritatea joburilor în 10 ani", "Tehnologie",
                        "Artificial intelligence will replace most jobs within 10 years", "Technology"},
                {"Partidele politice fac mai mult rău decât bine", "Politică",
                        "Political parties do more harm than good", "Politics"},
                {"Monarhia constituțională este depășită istoric", "Politică",
                        "Constitutional monarchy is historically outdated", "Politics"},
                {"Arta contemporană merită același respect ca arta clasică", "Cultură",
                        "Contemporary art deserves the same respect as classical art", "Culture"},
                {"Criptomonedele vor înlocui monedele naționale", "Economie",
                        "Cryptocurrencies will replace national currencies", "Economy"}
        };
        for (String[] t : topics) {
            debateTopicRepository.findByTitle(t[0]).ifPresentOrElse(topic -> {
                if (topic.getTitleEn() == null || topic.getTitleEn().isBlank()) {
                    topic.setTitleEn(t[2]);
                    topic.setCategoryEn(t[3]);
                    debateTopicRepository.save(topic);
                }
            }, () -> debateTopicRepository.save(DebateTopic.builder()
                    .title(t[0])
                    .category(t[1])
                    .titleEn(t[2])
                    .categoryEn(t[3])
                    .isActive(true)
                    .build()));
        }
    }

    private void seedFallacyGuides() {
        seedFallacy("Ad Hominem", "Atac la persoană", "Personal attack",
                "Atacă caracterul oponentului în loc de argument.",
                "Attacks the opponent's character instead of the argument.",
                "„Nu te ascult, ești prea tânăr să înțelegi economia.”",
                "\"I won't listen to you — you're too young to understand economics.\"",
                "Concentrează-te pe premisă și dovezi, nu pe identitate.",
                "Focus on premises and evidence, not identity.");
        seedFallacy("Strawman", "Distorsiune", "Distortion",
                "Exagerează sau simplifică greșit poziția adversarului.",
                "Exaggerates or misrepresents the opponent's position.",
                "„Deci vrei să interzici complet internetul?”",
                "\"So you want to ban the internet entirely?\"",
                "Reformulează fair poziția opusă înainte de a o critica.",
                "Fairly restate the opposing view before criticizing it.");
        seedFallacy("Appeal to Emotion", "Apel emoțional", "Appeal to emotion",
                "Înlocuiește logica cu teamă, milă sau mândrie.",
                "Replaces logic with fear, pity, or pride.",
                "„Dacă nu ești de acord, nu îți pasă de copii.”",
                "\"If you disagree, you don't care about children.\"",
                "Cere dovezi și lanț cauzal, nu doar imagini puternice.",
                "Ask for evidence and causal chains, not just vivid images.");
        seedFallacy("False Dilemma", "Falsă dilemă", "False dilemma",
                "Prezintă doar două opțiuni când există mai multe.",
                "Presents only two options when more exist.",
                "„Fie susținem AI total, fie rămânem în primitivism.”",
                "\"Either we fully embrace AI or we stay primitive.\"",
                "Caută soluții intermediare și nuanțe.",
                "Look for middle-ground solutions and nuance.");
        seedFallacy("Slippery Slope", "Panta alunecoasă", "Slippery slope",
                "Presupune un lanț inevitabil de evenimente negative.",
                "Assumes an inevitable chain of negative events.",
                "„Dacă permitem un protest, va urma haosul total.”",
                "\"If we allow one protest, total chaos will follow.\"",
                "Demonstrează fiecare pas din lanț cu probabilitate reală.",
                "Show each step in the chain with realistic probability.");
        seedFallacy("Hasty Generalization", "Generalizare grăbită", "Hasty generalization",
                "Concluzie globală din prea puține exemple.",
                "Draws a broad conclusion from too few examples.",
                "„Am cunoscut doi politicieni corupți, deci toți sunt corupți.”",
                "\"I met two corrupt politicians, so they are all corrupt.\"",
                "Folosește eșantioane reprezentative și statistici.",
                "Use representative samples and statistics.");
        seedFallacy("Appeal to Authority", "Apel la autoritate", "Appeal to authority",
                "„Un expert a spus” fără argument sau context.",
                "\"An expert said so\" without argument or context.",
                "„Un CEO celebru a zis, deci e adevărat.”",
                "\"A famous CEO said it, so it must be true.\"",
                "Verifică dacă autoritatea e relevantă și dacă există dovezi.",
                "Check if the authority is relevant and whether evidence exists.");
        seedFallacy("Circular Reasoning", "Raționament circular", "Circular reasoning",
                "Concluzia este și premisa.",
                "The conclusion is also the premise.",
                "„Biblia e adevărată pentru că Biblia spune că e adevărată.”",
                "\"The Bible is true because the Bible says it is true.\"",
                "Caută premize independente care susțin concluzia.",
                "Find independent premises that support the conclusion.");
        seedFallacy("Red Herring", "Diversiune", "Red herring",
                "Schimbă subiectul pentru a evita critica.",
                "Changes the subject to avoid the criticism.",
                "Întrebare despre buget, răspuns despre cariera personală.",
                "Question about the budget, answer about personal career.",
                "Revino la tema inițială și cere răspuns direct.",
                "Return to the original topic and ask for a direct answer.");
        seedFallacy("Tu Quoque", "Și tu la fel", "Tu quoque",
                "Respinge critica arătând că și criticul greșește.",
                "Dismisses criticism by showing the critic is also wrong.",
                "„Tu fumezi, deci nu poți critica poluarea.”",
                "\"You smoke, so you can't criticize pollution.\"",
                "Evaluează argumentul pe merit, nu pe ipocrizie.",
                "Judge the argument on its merits, not hypocrisy.");
        seedFallacy("Appeal to Ignorance", "Apel la ignoranță", "Appeal to ignorance",
                "„Nu e demonstrat fals, deci e adevărat.”",
                "\"It hasn't been proven false, therefore it's true.\"",
                "„Nimeni nu a dovedit că nu există extratereștri, deci există.”",
                "\"No one has proved aliens don't exist, so they exist.\"",
                "Absența dovezi nu înlocuiește dovada pozitivă.",
                "Absence of evidence is not positive evidence.");
        seedFallacy("False Cause", "Cauză falsă", "False cause",
                "Confundă corelația cu cauzalitatea.",
                "Confuses correlation with causation.",
                "„Vânzările de înghețată cresc odată cu inecțiile, deci înghețata provoacă inecții.”",
                "\"Ice cream sales rise with drownings, so ice cream causes drowning.\"",
                "Verifică mecanismul cauzal și variabile confuze.",
                "Check the causal mechanism and confounding variables.");
    }

    private void seedFallacy(String name, String category, String categoryEn, String description, String descriptionEn,
                             String example, String exampleEn, String howToAvoid, String howToAvoidEn) {
        fallacyGuideRepository.findByName(name).ifPresentOrElse(g -> {
            if (g.getDescriptionEn() == null || g.getDescriptionEn().isBlank()) {
                g.setCategoryEn(categoryEn);
                g.setDescriptionEn(descriptionEn);
                g.setExampleEn(exampleEn);
                g.setHowToAvoidEn(howToAvoidEn);
                fallacyGuideRepository.save(g);
            }
        }, () -> fallacyGuideRepository.save(FallacyGuide.builder()
                .name(name)
                .category(category)
                .categoryEn(categoryEn)
                .description(description)
                .descriptionEn(descriptionEn)
                .example(example)
                .exampleEn(exampleEn)
                .howToAvoid(howToAvoid)
                .howToAvoidEn(howToAvoidEn)
                .build()));
    }
}
