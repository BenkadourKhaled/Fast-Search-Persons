export type Language = 'fr' | 'ar';

export type Direction = 'ltr' | 'rtl';

export const LANGUAGE_STORAGE_KEY = 'fastsearch_language';

const fr = {
  'meta.title': 'FastSearch Persons · Moteur de recherche de personnes',

  'language.label': 'Langue',
  'language.switcherLabel': "Langue de l'interface · AR / FR / Translittérations",
  'language.fr': 'Français',
  'language.ar': 'العربية',

  'app.subtitle': 'Moteur de rapprochement de personnes · on-premise',
  'app.heroEyebrow': 'Recherche phonétique multilingue',
  'app.heroTitle1': 'Retrouvez une personne malgré les translittérations,',
  'app.heroTitle2': 'inversions et fautes de saisie.',
  'app.heroSubtitle': 'Interrogez en arabe, en français ou en mixte.',
  'app.syntheticData': 'Données synthétiques',
  'app.adminPanel': 'Panneau administration',
  'app.indexTab': 'Index',
  'app.auditTab': "Journal d'audit",
  'app.evalPanel': 'Panneau évaluation (données synthétiques)',
  'app.filteredSuffix': '({count} après filtre local)',
  'app.confidenceDistribution': 'Répartition des niveaux de confiance sur la page courante',
  'app.exportJson': 'Export JSON',
  'app.engineWarnings': 'Avertissements du moteur',
  'app.resultPrudent': 'Résultat à interpréter avec prudence',
  'app.localFilter': 'Filtre local sur la page :',
  'app.all': 'Tous',
  'app.fort': 'Fort',
  'app.moyen': 'Moyen',
  'app.faible': 'Faible',
  'app.noCandidateAtLevel': 'Aucun candidat avec ce niveau sur cette page',
  'app.noCandidateAtLevelHint':
    "Le filtre de confiance ne s'applique qu'aux résultats de la page courante.",
  'app.footerBackend': 'connecté au backend {url}',
  'app.footerProxy': 'connecté via le proxy Vite (même origine)',
  'app.footerText':
    "FastSearch Persons — {connection}. API REST v1. Aucune donnée personnelle réelle n'est stockée. Le résultat est un candidat, jamais une identification.",
  'app.unexpectedError': 'Erreur inattendue',
  loading: 'Recherche en cours…',

  'login.subtitle': 'Moteur de rapprochement de personnes',
  'login.username': 'Identifiant',
  'login.password': 'Mot de passe',
  'login.usernamePlaceholder': 'prenom.nom@fastsearch.test',
  'login.submit': 'Se connecter',
  'login.loading': 'Connexion…',
  'login.errorRequired': 'Veuillez saisir un identifiant et un mot de passe.',
  'login.errorFailed': 'Échec de connexion.',
  'login.basicNote':
    "Authentification HTTP Basic. L'en-tête est conservé uniquement dans le sessionStorage de cet onglet, jamais dans le localStorage.",
  'login.hideConfig': 'Masquer la configuration',
  'login.showConfig': 'Configurer le backend',
  'login.backendUrl': 'URL du backend',
  'login.emptySameOrigin': 'Vide = même origine',
  'login.apply': 'Appliquer',
  'login.configHint':
    "Laissez vide en développement : Vite relaie /api et /actuator vers http://localhost:8080. Pour une autre origine, le backend doit l'autoriser via fastsearch.cors.allowed-origins.",
  'login.proxyEndpoint': 'Proxy Vite · même origine',
  'login.apiNote':
    "Les appels utilisent l'API REST v1 et les identités du backend Spring Boot.",
  'login.syntheticFooter':
    'Données 100 % synthétiques. Aucune personne réelle. Le résultat est un candidat, jamais une identification.',

  'search.title': 'Recherche de personnes',
  'search.structured': 'Structurée',
  'search.free': 'Texte libre',
  'search.freeLabel': 'Requête en texte libre',
  'search.freePlaceholder': 'ex. Oualid Benali 07/04/1992 · وليد بن علي · Karim 23041989',
  'search.freeHint':
    "Saisissez nom, prénom et/ou date dans n'importe quel ordre. Les hypothèses d'inversion sont générées automatiquement.",
  'search.lastName': 'Nom',
  'search.firstName': 'Prénom',
  'search.birthDate': 'Date de naissance',
  'search.birthPlace': 'Lieu de naissance',
  'search.lastNamePlaceholder': 'Benali, بن علي…',
  'search.firstNamePlaceholder': 'Walid, Oualid, وليد…',
  'search.birthDatePlaceholder': '07/04/1992, 07041992, 070499…',
  'search.birthPlacePlaceholder': 'Alger, Oran…',
  'search.reset': 'Réinitialiser',
  'search.submit': 'Rechercher',

  'result.unknownName': 'Nom inconnu',
  'result.father': 'Père : {name}',
  'result.mother': 'Mère : {name}',
  'result.group': 'Groupe {group}',
  'result.fieldsExplained': '{count} champ(s) expliqué(s)',
  'result.explanationToggle': 'Explication champ par champ ({count})',
  'result.noExplanation': 'Aucune explication renvoyée par le backend pour ce candidat.',
  'result.similarity': 'similarité',
  'result.candidateNotice':
    'Ce résultat est un CANDIDAT, jamais une identification. Le score mesure une similarité entre la requête et les champs indexés ; la décision reste humaine.',

  'drawer.title': 'Détail du candidat',
  'drawer.close': 'Fermer',
  'drawer.unknownDate': 'Date inconnue',
  'drawer.sourceId': 'ID source',
  'drawer.group': 'Groupe',
  'drawer.language': 'Langue',
  'drawer.birth': 'Naissance',
  'drawer.place': 'Lieu',
  'drawer.father': 'Père',
  'drawer.mother': 'Mère',
  'drawer.explanation': 'Explication champ par champ',
  'drawer.noExplanation': 'Aucune explication renvoyée par le backend pour ce candidat.',
  'drawer.candidateStrong': 'Candidat, pas identification.',
  'drawer.candidateNotice':
    "Ce résultat indique une similarité mesurée entre la requête et un enregistrement. La décision d'identifier une personne reste humaine et contextuelle.",

  'empty.idleTitle': "Aucune recherche pour l'instant",
  'empty.idleText':
    "Saisissez un nom, un prénom ou une date pour lancer la recherche. Les résultats afficheront un score, un niveau de confiance et l'explication champ par champ.",
  'empty.vagueTitle': 'Requête trop vague',
  'empty.vagueText':
    "Une requête vide ou trop imprécise ne renvoie pas toute la base. Précisez au moins un nom ou une date de naissance.",
  'empty.noResultTitle': 'Aucun résultat fiable',
  'empty.noResultText':
    "Aucun candidat n'a dépassé le seuil de pertinence. Essayez une variante orthographique, une translittération ou une date partielle.",

  'banner.strong': 'Rappel éthique :',
  'banner.text':
    "chaque résultat est un candidat, jamais une identification. Un score élevé indique une forte similarité, pas une certitude. La décision d'identifier une personne reste humaine et contextuelle.",

  'pagination.range': '{from}–{to} sur {total}',
  'pagination.perPage': 'par page',
  'pagination.previous': 'Page précédente',
  'pagination.next': 'Page suivante',

  'admin.title': "Administration de l'index",
  'admin.description':
    "La base source reste la vérité ; l'index Lucene est reconstruisible. La synchronisation committe après chaque lot. Aucun endpoint de « commit » manuel n'est exposé.",
  'admin.errorStats': 'Erreur lors de la lecture des statistiques.',
  'admin.errorReindex': 'Réindexation impossible.',
  'admin.errorSync': 'Synchronisation impossible.',
  'admin.errorSince': 'La date de départ de la synchronisation est invalide.',
  'admin.refreshStats': 'Rafraîchir les statistiques',
  'admin.reindex': 'Réindexation complète',
  'admin.sync': 'Synchronisation incrémentale',
  'admin.sinceLabel': 'Départ incrémental (optionnel, heure locale)',
  'admin.sinceHint':
    "Sans date, le backend lit depuis l'origine et le signale dans les avertissements.",
  'admin.documents': 'Documents',
  'admin.maxDoc': 'Max doc',
  'admin.deleted': 'Supprimés',
  'admin.state': 'État',
  'admin.open': 'Ouvert',
  'admin.closed': 'Fermé',
  'admin.reindexDone': 'Réindexation terminée',
  'admin.syncDone': 'Synchronisation terminée',
  'admin.reportSummary':
    '{indexed} document(s) indexé(s), {deleted} supprimé(s), {rowsRead} ligne(s) lue(s) en {batches} lot(s). Durée : {duration}.',
  'admin.startedAt': 'Démarré le {date}',
  'admin.lastId': ' · dernier identifiant : {id}',

  'audit.title': "Journal d'audit",
  'audit.refresh': 'Actualiser',
  'audit.description':
    "Chaque recherche et action d'administration est journalisée : qui, quand, type de requête, nombre de résultats et durée. Seule une empreinte SHA-256 de la requête est conservée ; aucune requête brute ni donnée personnelle n'est écrite dans le journal.",
  'audit.error': 'Lecture du journal impossible.',
  'audit.empty': 'Aucune entrée dans le journal.',
  'audit.timestamp': 'Horodatage',
  'audit.user': 'Utilisateur',
  'audit.action': 'Action',
  'audit.hash': 'Empreinte',
  'audit.results': 'Résultats',
  'audit.confidence': 'Confiance',
  'audit.duration': 'Durée',
  'audit.status': 'État',
  'audit.noQuery': 'Aucune requête associée',
  'audit.entries': '{count} entrée(s)',
  'audit.previous': 'Page précédente',
  'audit.next': 'Page suivante',

  'eval.title': 'Évaluation synthétique',
  'eval.description':
    "Jeu de cas généré de façon reproductible à partir d'une graine, sur des données 100 % synthétiques. Les métriques mesurent le rappel et le classement du moteur, pas sa performance sur des données réelles.",
  'eval.errorSeed': 'La graine doit être un entier.',
  'eval.errorCount': 'Le nombre de cas doit être un entier compris entre 12 et 120.',
  'eval.errorRun': "L'évaluation a échoué.",
  'eval.seed': 'Graine',
  'eval.count': 'Nombre de cas (12–120)',
  'eval.run': "Lancer l'évaluation",
  'eval.report':
    'Rapport du {date} · graine {seed} · {personCount} personnes synthétiques · {caseCount} cas · top {topK}',
  'eval.recall': 'Recall@10',
  'eval.mrr': 'MRR',
  'eval.ndcg': 'nDCG@10',
  'eval.precision': 'Précision@1',
  'eval.falseStrong': 'Faux forts',
  'eval.falseStrongHint': '{falseStrong} sur {strongHits} forts',
  'eval.byCategory': 'Métriques par catégorie',
  'eval.category': 'Catégorie',
  'eval.cases': 'Cas',
  'eval.strong': 'Forts',
  'eval.falseStrongPlural': 'Faux forts',
  'eval.baselines': 'Comparaison aux baselines',
  'eval.available': 'Disponible',
  'eval.unavailable': 'Indisponible',
  'eval.suggestions': 'Suggestions pour le dictionnaire',
  'eval.noSuggestion': 'Aucune suggestion pour cette graine.',
  'eval.occurrences': '{count} occurrence(s)',
  'eval.suggestionNote':
    "Ces lignes sont des propositions à relire humainement avant tout ajout au dictionnaire de variantes. Aucune modification automatique n'est appliquée.",
  'eval.categoryExact': 'Exact',
  'eval.categoryInversion': 'Inversion nom/prénom',
  'eval.categoryArabic': 'Arabe',
  'eval.categoryTransliteration': 'Translittération',
  'eval.categoryTypo': 'Faute de frappe / OCR',
  'eval.categoryDate': 'Date',
  'eval.categoryHomonym': 'Homonymes',

  'settings.live': 'Live',
  'settings.roleAdmin': 'Administrateur',
  'settings.roleAnalyst': 'Analyste',
  'settings.roleReader': 'Lecteur',
  'settings.configTitle': 'Configuration backend',
  'settings.backendUrl': 'URL backend',
  'settings.backendUrlHint': 'Vide = même origine (proxy Vite). Sinon, URL absolue du backend.',
  'settings.logout': 'Se déconnecter',
  'settings.open': 'Ouvrir les paramètres',

  'display.fieldIdentifiant': 'Identifiant',
  'display.fieldNom': 'Nom',
  'display.fieldPrenom': 'Prénom',
  'display.fieldNomComplet': 'Nom complet',
  'display.fieldNomFamille': 'Nom de famille',
  'display.fieldDateNaissance': 'Date de naissance',
  'display.fieldLieuNaissance': 'Lieu de naissance',
  'display.levelExact': 'Exact',
  'display.levelTranslit': 'Translittéré / dictionnaire',
  'display.levelPhonFine': 'Phonétique fine',
  'display.levelPhonGrossiere': 'Phonétique grossière',
  'display.levelFlou': 'Flou toléré',
  'display.levelAucun': 'Aucune correspondance',
  'display.levelIncomplet': 'Nom incomplet',
  'display.levelDateExacte': 'Date exacte',
  'display.levelJourMoisInverse': 'Jour/mois inversé',
  'display.levelTolee': 'Date tolérée',
  'display.levelPartielle': 'Date partielle',
  'display.levelHypothese': 'Hypothèse',
  'display.levelIncompatible': 'Incompatible',
  'display.dateUnknown': 'Date inconnue',
  'display.precisionYear': 'Année',
  'display.precisionMonth': 'Mois',
  'display.precisionDay': 'Jour',
  'display.precisionUnknown': 'Inconnue',
  'display.langFr': 'Français',
  'display.langAr': 'Arabe',
  'display.langMixed': 'Mixte',
  'display.confidenceFort': 'Confiance forte',
  'display.confidenceMoyen': 'Confiance moyenne',
  'display.confidenceFaible': 'Confiance faible',
} as const;

export type TranslationKey = keyof typeof fr;

const ar: Record<TranslationKey, string> = {
  'meta.title': 'FastSearch Persons · محرك البحث عن الأشخاص',

  'language.label': 'اللغة',
  'language.switcherLabel': 'لغة الواجهة · AR / FR / النقل الحرفي',
  'language.fr': 'الفرنسية',
  'language.ar': 'العربية',

  'app.subtitle': 'محرك مطابقة الأشخاص · داخل المنشأة',
  'app.heroEyebrow': 'بحث صوتي متعدد اللغات',
  'app.heroTitle1': 'اعثر على شخص رغم اختلاف النقل الحرفي،',
  'app.heroTitle2': 'والانعكاسات وأخطاء الإدخال.',
  'app.heroSubtitle': 'ابحث بالعربية أو بالفرنسية أو بمزيج منهما.',
  'app.syntheticData': 'بيانات اصطناعية',
  'app.adminPanel': 'لوحة الإدارة',
  'app.indexTab': 'الفهرس',
  'app.auditTab': 'سجل التدقيق',
  'app.evalPanel': 'لوحة التقييم (بيانات اصطناعية)',
  'app.filteredSuffix': '({count} بعد التصفية المحلية)',
  'app.confidenceDistribution': 'توزيع مستويات الثقة في الصفحة الحالية',
  'app.exportJson': 'تصدير JSON',
  'app.engineWarnings': 'تحذيرات المحرك',
  'app.resultPrudent': 'نتيجة يجب تفسيرها بحذر',
  'app.localFilter': 'تصفية محلية في الصفحة:',
  'app.all': 'الكل',
  'app.fort': 'قوية',
  'app.moyen': 'متوسطة',
  'app.faible': 'ضعيفة',
  'app.noCandidateAtLevel': 'لا يوجد مرشح بهذا المستوى في هذه الصفحة',
  'app.noCandidateAtLevelHint': 'لا تُطبق تصفية الثقة إلا على نتائج الصفحة الحالية.',
  'app.footerBackend': 'متصل بالخادم {url}',
  'app.footerProxy': 'متصل عبر وكيل Vite (نفس المصدر)',
  'app.footerText':
    'FastSearch Persons — {connection}. واجهة REST v1. لا تُخزَّن أي بيانات شخصية حقيقية. النتيجة مرشح، وليست تحديد هوية.',
  'app.unexpectedError': 'خطأ غير متوقع',
  loading: 'جارٍ البحث…',

  'login.subtitle': 'محرك مطابقة الأشخاص',
  'login.username': 'اسم المستخدم',
  'login.password': 'كلمة المرور',
  'login.usernamePlaceholder': 'prenom.nom@fastsearch.test',
  'login.submit': 'تسجيل الدخول',
  'login.loading': 'جارٍ تسجيل الدخول…',
  'login.errorRequired': 'يرجى إدخال اسم المستخدم وكلمة المرور.',
  'login.errorFailed': 'فشل تسجيل الدخول.',
  'login.basicNote':
    'مصادقة HTTP Basic. تُحفظ الترويسة فقط في sessionStorage لهذا التبويب، وليس في localStorage.',
  'login.hideConfig': 'إخفاء الإعدادات',
  'login.showConfig': 'إعداد الخادم',
  'login.backendUrl': 'عنوان URL للخادم',
  'login.emptySameOrigin': 'فارغ = نفس المصدر',
  'login.apply': 'تطبيق',
  'login.configHint':
    'اتركه فارغًا أثناء التطوير: يعيد Vite توجيه /api و/actuator إلى http://localhost:8080. لمصدر آخر، يجب أن يسمح به الخادم عبر fastsearch.cors.allowed-origins.',
  'login.proxyEndpoint': 'وكيل Vite · نفس المصدر',
  'login.apiNote': 'تستخدم الاستدعاءات واجهة REST v1 وهويات خادم Spring Boot.',
  'login.syntheticFooter':
    'بيانات اصطناعية 100%. لا يوجد أي شخص حقيقي. النتيجة مرشح، وليست تحديد هوية.',

  'search.title': 'البحث عن الأشخاص',
  'search.structured': 'منظم',
  'search.free': 'نص حر',
  'search.freeLabel': 'استعلام نصي حر',
  'search.freePlaceholder': 'مثال: وليد بن علي 07/04/1992 · Oualid Benali · Karim 23041989',
  'search.freeHint':
    'أدخل الاسم واللقب و/أو التاريخ بأي ترتيب. تُنشأ فرضيات الانعكاس تلقائيًا.',
  'search.lastName': 'اللقب',
  'search.firstName': 'الاسم',
  'search.birthDate': 'تاريخ الميلاد',
  'search.birthPlace': 'مكان الميلاد',
  'search.lastNamePlaceholder': 'بن علي، Benali…',
  'search.firstNamePlaceholder': 'وليد، Walid، Oualid…',
  'search.birthDatePlaceholder': '07/04/1992، 07041992، 070499…',
  'search.birthPlacePlaceholder': 'الجزائر، وهران…',
  'search.reset': 'إعادة تعيين',
  'search.submit': 'بحث',

  'result.unknownName': 'اسم غير معروف',
  'result.father': 'الأب: {name}',
  'result.mother': 'الأم: {name}',
  'result.group': 'المجموعة {group}',
  'result.fieldsExplained': 'شرح {count} من الحقول',
  'result.explanationToggle': 'الشرح حقلاً بحقل ({count})',
  'result.noExplanation': 'لا يوجد شرح من الخادم لهذا المرشح.',
  'result.similarity': 'التشابه',
  'result.candidateNotice':
    'هذه النتيجة مرشح، وليست تحديد هوية. تقيس الدرجة التشابه بين الاستعلام والحقول المفهرسة؛ ويبقى القرار بشريًا.',

  'drawer.title': 'تفاصيل المرشح',
  'drawer.close': 'إغلاق',
  'drawer.unknownDate': 'تاريخ غير معروف',
  'drawer.sourceId': 'المعرّف المصدر',
  'drawer.group': 'المجموعة',
  'drawer.language': 'اللغة',
  'drawer.birth': 'الميلاد',
  'drawer.place': 'المكان',
  'drawer.father': 'الأب',
  'drawer.mother': 'الأم',
  'drawer.explanation': 'الشرح حقلاً بحقل',
  'drawer.noExplanation': 'لا يوجد شرح من الخادم لهذا المرشح.',
  'drawer.candidateStrong': 'مرشح، وليس تحديد هوية.',
  'drawer.candidateNotice':
    'تشير هذه النتيجة إلى تشابه مقاس بين الاستعلام وسجل. يبقى قرار تحديد هوية الشخص بشريًا ومرتبطًا بالسياق.',

  'empty.idleTitle': 'لا يوجد بحث بعد',
  'empty.idleText':
    'أدخل اسمًا أو لقبًا أو تاريخًا لبدء البحث. ستعرض النتائج درجة ومستوى ثقة وشرحًا حقلاً بحقل.',
  'empty.vagueTitle': 'الاستعلام غامض جدًا',
  'empty.vagueText':
    'لا يعيد الاستعلام الفارغ أو غير الدقيق كامل قاعدة البيانات. حدد على الأقل اسمًا أو تاريخ ميلاد.',
  'empty.noResultTitle': 'لا توجد نتائج موثوقة',
  'empty.noResultText':
    'لم يتجاوز أي مرشح حد الصلة. جرّب صيغة إملائية بديلة أو نقلًا حرفيًا أو تاريخًا جزئيًا.',

  'banner.strong': 'تذكير أخلاقي:',
  'banner.text':
    'كل نتيجة مرشح، وليست تحديد هوية. الدرجة العالية تعني تشابهًا قويًا، وليست يقينًا. يبقى قرار تحديد هوية الشخص بشريًا ومرتبطًا بالسياق.',

  'pagination.range': '{from}–{to} من {total}',
  'pagination.perPage': 'لكل صفحة',
  'pagination.previous': 'الصفحة السابقة',
  'pagination.next': 'الصفحة التالية',

  'admin.title': 'إدارة الفهرس',
  'admin.description':
    'تبقى قاعدة المصدر هي المرجع؛ فهرس Lucene قابل لإعادة البناء. تُثبَّت المزامنة بعد كل دفعة. لا توجد نقطة نهاية «commit» يدوية.',
  'admin.errorStats': 'خطأ أثناء قراءة الإحصائيات.',
  'admin.errorReindex': 'تعذرت إعادة الفهرسة.',
  'admin.errorSync': 'تعذرت المزامنة.',
  'admin.errorSince': 'تاريخ بدء المزامنة غير صالح.',
  'admin.refreshStats': 'تحديث الإحصائيات',
  'admin.reindex': 'إعادة فهرسة كاملة',
  'admin.sync': 'مزامنة تزايدية',
  'admin.sinceLabel': 'بداية تزايدية (اختياري، بالتوقيت المحلي)',
  'admin.sinceHint': 'بدون تاريخ، يقرأ الخادم من البداية وينبّه إلى ذلك في التحذيرات.',
  'admin.documents': 'الوثائق',
  'admin.maxDoc': 'الحد الأقصى للوثائق',
  'admin.deleted': 'المحذوفة',
  'admin.state': 'الحالة',
  'admin.open': 'مفتوح',
  'admin.closed': 'مغلق',
  'admin.reindexDone': 'اكتملت إعادة الفهرسة',
  'admin.syncDone': 'اكتملت المزامنة',
  'admin.reportSummary':
    'تمت فهرسة {indexed} وثيقة، وحذف {deleted}، وقراءة {rowsRead} صفًا في {batches} دفعة. المدة: {duration}.',
  'admin.startedAt': 'بدأ في {date}',
  'admin.lastId': ' · آخر معرّف: {id}',

  'audit.title': 'سجل التدقيق',
  'audit.refresh': 'تحديث',
  'audit.description':
    'تُسجَّل كل عملية بحث وإجراء إداري: من، ومتى، ونوع الاستعلام، وعدد النتائج، والمدة. يُحتفظ فقط ببصمة SHA-256 للاستعلام؛ ولا يُكتب أي استعلام خام أو بيانات شخصية في السجل.',
  'audit.error': 'تعذرت قراءة السجل.',
  'audit.empty': 'لا توجد مدخلات في السجل.',
  'audit.timestamp': 'الطابع الزمني',
  'audit.user': 'المستخدم',
  'audit.action': 'الإجراء',
  'audit.hash': 'البصمة',
  'audit.results': 'النتائج',
  'audit.confidence': 'الثقة',
  'audit.duration': 'المدة',
  'audit.status': 'الحالة',
  'audit.noQuery': 'لا يوجد استعلام مرتبط',
  'audit.entries': '{count} مدخل',
  'audit.previous': 'الصفحة السابقة',
  'audit.next': 'الصفحة التالية',

  'eval.title': 'التقييم الاصطناعي',
  'eval.description':
    'مجموعة حالات تُولَّد بشكل قابل للتكرار من بذرة، على بيانات اصطناعية 100%. تقيس المقاييس استدعاء المحرك وترتيبه، وليس أداءه على بيانات حقيقية.',
  'eval.errorSeed': 'يجب أن تكون البذرة عددًا صحيحًا.',
  'eval.errorCount': 'يجب أن يكون عدد الحالات عددًا صحيحًا بين 12 و120.',
  'eval.errorRun': 'فشل التقييم.',
  'eval.seed': 'البذرة',
  'eval.count': 'عدد الحالات (12–120)',
  'eval.run': 'تشغيل التقييم',
  'eval.report':
    'تقرير بتاريخ {date} · البذرة {seed} · {personCount} أشخاص اصطناعيون · {caseCount} حالة · أفضل {topK}',
  'eval.recall': 'الاستدعاء@10',
  'eval.mrr': 'MRR',
  'eval.ndcg': 'nDCG@10',
  'eval.precision': 'الدقة@1',
  'eval.falseStrong': 'القوية الخاطئة',
  'eval.falseStrongHint': '{falseStrong} من {strongHits} قوية',
  'eval.byCategory': 'المقاييس حسب الفئة',
  'eval.category': 'الفئة',
  'eval.cases': 'الحالات',
  'eval.strong': 'القوية',
  'eval.falseStrongPlural': 'القوية الخاطئة',
  'eval.baselines': 'المقارنة مع خطوط الأساس',
  'eval.available': 'متاح',
  'eval.unavailable': 'غير متاح',
  'eval.suggestions': 'اقتراحات للقاموس',
  'eval.noSuggestion': 'لا توجد اقتراحات لهذه البذرة.',
  'eval.occurrences': '{count} تكرار',
  'eval.suggestionNote':
    'هذه الأسطر اقتراحات يجب مراجعتها بشريًا قبل أي إضافة إلى قاموس المتغيرات. لا يُطبق أي تعديل تلقائي.',
  'eval.categoryExact': 'مطابق',
  'eval.categoryInversion': 'انعكاس اللقب/الاسم',
  'eval.categoryArabic': 'عربي',
  'eval.categoryTransliteration': 'نقل حرفي',
  'eval.categoryTypo': 'خطأ إملائي / OCR',
  'eval.categoryDate': 'التاريخ',
  'eval.categoryHomonym': 'متشابهو الأسماء',

  'settings.live': 'مباشر',
  'settings.roleAdmin': 'مدير',
  'settings.roleAnalyst': 'محلل',
  'settings.roleReader': 'قارئ',
  'settings.configTitle': 'إعداد الخادم',
  'settings.backendUrl': 'عنوان URL للخادم',
  'settings.backendUrlHint': 'فارغ = نفس المصدر (وكيل Vite). وإلا، عنوان URL المطلق للخادم.',
  'settings.logout': 'تسجيل الخروج',
  'settings.open': 'فتح الإعدادات',

  'display.fieldIdentifiant': 'المعرّف',
  'display.fieldNom': 'اللقب',
  'display.fieldPrenom': 'الاسم',
  'display.fieldNomComplet': 'الاسم الكامل',
  'display.fieldNomFamille': 'اسم العائلة',
  'display.fieldDateNaissance': 'تاريخ الميلاد',
  'display.fieldLieuNaissance': 'مكان الميلاد',
  'display.levelExact': 'مطابق',
  'display.levelTranslit': 'منقول حرفيًا / القاموس',
  'display.levelPhonFine': 'صوتي دقيق',
  'display.levelPhonGrossiere': 'صوتي تقريبي',
  'display.levelFlou': 'تشابه ضبابي مسموح',
  'display.levelAucun': 'لا تطابق',
  'display.levelIncomplet': 'اسم غير كامل',
  'display.levelDateExacte': 'تاريخ مطابق',
  'display.levelJourMoisInverse': 'انعكاس اليوم/الشهر',
  'display.levelTolee': 'تاريخ مقبول',
  'display.levelPartielle': 'تاريخ جزئي',
  'display.levelHypothese': 'فرضية',
  'display.levelIncompatible': 'غير متوافق',
  'display.dateUnknown': 'تاريخ غير معروف',
  'display.precisionYear': 'سنة',
  'display.precisionMonth': 'شهر',
  'display.precisionDay': 'يوم',
  'display.precisionUnknown': 'غير معروفة',
  'display.langFr': 'الفرنسية',
  'display.langAr': 'العربية',
  'display.langMixed': 'مختلط',
  'display.confidenceFort': 'ثقة قوية',
  'display.confidenceMoyen': 'ثقة متوسطة',
  'display.confidenceFaible': 'ثقة ضعيفة',
};

const dictionaries: Record<Language, Record<TranslationKey, string>> = { fr, ar };

export function translate(
  lang: Language,
  key: TranslationKey,
  vars?: Record<string, string | number>
): string {
  let value = dictionaries[lang][key] ?? dictionaries.fr[key] ?? key;
  if (vars) {
    for (const [name, replacement] of Object.entries(vars)) {
      value = value.split(`{${name}}`).join(String(replacement));
    }
  }
  return value;
}

export function candidateCountLabel(lang: Language, count: number): string {
  if (lang === 'fr') return `${count} candidat${count > 1 ? 's' : ''}`;
  if (count === 1) return 'مرشح واحد';
  if (count === 2) return 'مرشحان';
  if (count >= 3 && count <= 10) return `${count} مرشحين`;
  return `${count} مرشحًا`;
}

/**
 * Les messages, avertissements et détails d'explication proviennent du backend
 * en français. On les traduit à l'affichage pour éviter un mélange de langues.
 */
const BACKEND_FR_TO_AR: Record<string, string> = {
  'Aucun candidat ne correspond aux critères.': 'لا يوجد مرشح يطابق المعايير.',
  'Candidats proposés ; aucun résultat ne constitue une identification.':
    'مرشحون مقترحون؛ لا تشكل أي نتيجة تحديد هوية.',
  'Résultats faibles : aucun candidat fiable, vérification manuelle requise.':
    'نتائج ضعيفة: لا يوجد مرشح موثوق، ويلزم التحقق اليدوي.',
  'aucun groupe de lecture autorisé': 'لا توجد مجموعة قراءة مصرح بها',
  'requête trop vague : aucun critère exploitable':
    'الاستعلام غامض جدًا: لا يوجد معيار قابل للاستخدام',
  "ambiguïté numérique : la date et l'identifiant ont tous deux été recherchés":
    'التباس رقمي: تم البحث عن التاريخ والمعرّف معًا',
  "Erreur d'accès à l'index Lucene": 'خطأ في الوصول إلى فهرس Lucene',
  "Erreur de lecture des statistiques d'index": 'خطأ في قراءة إحصائيات الفهرس',
  "Erreur de commit de l'index": 'خطأ في تثبيت الفهرس',
  'Évaluation impossible : accès à l’index temporaire': 'تعذر التقييم: الوصول إلى الفهرس المؤقت',
  "Évaluation impossible : accès à l'index temporaire": 'تعذر التقييم: الوصول إلى الفهرس المؤقت',

  'date de naissance absente du candidat': 'تاريخ الميلاد غير موجود لدى المرشح',
  'identifiant interne exact': 'المعرّف الداخلي مطابق',
  'nom de famille réduit à une particule de filiation : confiance plafonnée à MOYEN':
    'اسم العائلة مختزل إلى أداة نسب: الثقة محدودة إلى متوسطة',
  'date absente': 'التاريخ غير موجود',
  'comparaison du lieu normalisé': 'مقارنة المكان بعد التطبيع',
  'lieu incompatible': 'المكان غير متوافق',
  'comparaison des deux ordres': 'مقارنة الترتيبين',
  'inversion nom/prénom prise en compte': 'تم أخذ انعكاس اللقب/الاسم في الاعتبار',
  'comparaison directe': 'مقارنة مباشرة',
  'comparaison du nom': 'مقارنة الاسم',
  exact: 'مطابق',
  'translittéré / variante de dictionnaire': 'منقول حرفيًا / متغير قاموسي',
  'phonétique fine': 'صوتي دقيق',
  'phonétique grossière (plafonnée)': 'صوتي تقريبي (بحد أقصى)',
  'flou toléré': 'تشابه ضبابي مسموح',
  'aucune correspondance': 'لا تطابق',
  'date exacte': 'تاريخ مطابق',
  'jour et mois inversés': 'انعكاس اليوم والشهر',
  'année à ±1': 'سنة بفارق ±1',
  'une erreur de chiffre tolérée': 'خطأ رقمي واحد مسموح',
  'dates incompatibles': 'تواريخ غير متوافقة',
  'année compatible (date partielle)': 'سنة متوافقة (تاريخ جزئي)',
  'années différentes': 'سنوات مختلفة',
  'mois exact': 'شهر مطابق',
  'même année, mois différent': 'نفس السنة، شهر مختلف',
  'mois incompatibles': 'أشهر غير متوافقة',
  'mois compatible (date partielle)': 'شهر متوافق (تاريخ جزئي)',
  'dates partielles incompatibles': 'تواريخ جزئية غير متوافقة',

  'aucun point de terminaison ou nom d’index valide configuré':
    'لم يتم تكوين نقطة نهاية أو اسم فهرس صالح',
  "aucun point de terminaison ou nom d'index valide configuré":
    'لم يتم تكوين نقطة نهاية أو اسم فهرس صالح',
  'multi_match sur les champs nominaux, sans phonétique locale':
    'multi_match على الحقول الاسمية، دون صوتيات محلية',
  'sous-chaîne normalisée, sans phonétique ni dictionnaire':
    'سلسلة فرعية مطبعة، دون صوتيات أو قاموس',

  'Connexion impossible': 'تعذر الاتصال',
  "Le backend n'est pas joignable. Vérifiez qu'il écoute sur le port 8080 ou l'URL configurée.":
    'تعذر الوصول إلى الخادم. تحقق من أنه يستمع على المنفذ 8080 أو على العنوان المكوَّن.',
  'Identifiants invalides ou session expirée.': 'بيانات اعتماد غير صالحة أو جلسة منتهية.',
  'Le backend exige une authentification HTTP Basic valide.':
    'يتطلب الخادم مصادقة HTTP Basic صالحة.',
  'Accès refusé': 'تم رفض الوصول',
  'Votre rôle ne permet pas cette opération. Un administrateur peut vous accorder les droits nécessaires.':
    'دورك لا يسمح بهذه العملية. يمكن لمسؤول منحك الحقوق اللازمة.',
  'Erreur lors de la lecture des statistiques.': 'خطأ أثناء قراءة الإحصائيات.',
  'Réindexation impossible.': 'تعذرت إعادة الفهرسة.',
  'Synchronisation impossible.': 'تعذرت المزامنة.',
  'Lecture du journal impossible.': 'تعذرت قراءة السجل.',
  "L'évaluation a échoué.": 'فشل التقييم.',
  'La graine doit être un entier.': 'يجب أن تكون البذرة عددًا صحيحًا.',
  'Le nombre de cas doit être un entier compris entre 12 et 120.':
    'يجب أن يكون عدد الحالات عددًا صحيحًا بين 12 و120.',
  'La date de départ de la synchronisation est invalide.': 'تاريخ بدء المزامنة غير صالح.',
  'Échec de connexion.': 'فشل تسجيل الدخول.',
  'Veuillez saisir un identifiant et un mot de passe.':
    'يرجى إدخال اسم المستخدم وكلمة المرور.',
  'Erreur inattendue': 'خطأ غير متوقع',
};

export function localizeBackendText(text: string, lang: Language): string {
  if (!text || lang === 'fr') return text;
  const trimmed = text.trim();
  const direct = BACKEND_FR_TO_AR[trimmed];
  if (direct) return direct;

  const limit = /^recherche limitée aux (\d+) meilleurs candidats Lucene avant reranking$/.exec(
    trimmed
  );
  if (limit) {
    return `البحث محدود بأفضل ${limit[1]} مرشح من Lucene قبل إعادة الترتيب`;
  }

  const errorCode = /^Erreur (\d+)$/.exec(trimmed);
  if (errorCode) return `خطأ ${errorCode[1]}`;

  const requestPath = /^Requête : (.+)$/.exec(trimmed);
  if (requestPath) return `الطلب: ${requestPath[1]}`;

  const openSearchCall = /^appel impossible \((.+)\)$/.exec(trimmed);
  if (openSearchCall) return `استدعاء غير ممكن (${openSearchCall[1]})`;

  const conventionalDate = ' ; date 01/01 conventionnelle plafonnée';
  if (trimmed.endsWith(conventionalDate)) {
    const base = trimmed.slice(0, -conventionalDate.length);
    return `${localizeBackendText(base, lang)} ; تاريخ 01/01 اصطلاحي بحد أقصى`;
  }

  return text;
}
