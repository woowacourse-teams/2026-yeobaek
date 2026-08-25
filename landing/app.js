"use strict";

const BOOK = {
  title: "이상한 나라의 앨리스",
  author: "루이스 캐럴",
  chapter: "제1장 토끼 굴 속으로",
  passages: [
    "앨리스는 언니 곁에서 둑에 앉아 아무 할 일도 없이 지내는 것이 슬슬 지겨워졌다. 언니가 읽는 책을 한두 번 들여다보았지만 그림도 없고 대화도 없었다. ‘그림도 대화도 없는 책이 무슨 소용이지?’ 앨리스는 생각했다.",
    "그래서 데이지 꽃목걸이를 만들면 즐겁기는 하겠지만, 굳이 일어나 꽃을 따러 갈 만큼 즐거울까 하고 혼자 곰곰이 따져 보던 참이었다. 더운 날씨 탓에 몹시 졸리고 머리도 멍해서, 생각이라 해 봐야 제대로 되지는 않았다. 그때 갑자기 분홍빛 눈을 가진 흰 토끼 한 마리가 앨리스 곁을 바짝 스쳐 달려갔다.",
    "그것만으로는 그다지 놀랄 일이 아니었다. 토끼가 혼잣말로 “이런, 이런! 늦겠어!”라고 하는 것을 듣고도 앨리스는 별로 이상하게 여기지 않았다. 나중에 돌이켜 보고서야 마땅히 놀랐어야 했다는 생각이 들었지만, 그 순간에는 모두 아주 자연스러워 보였다. 하지만 토끼가 정말로 조끼 주머니에서 시계를 꺼내 들여다본 다음 다시 서둘러 가자, 앨리스는 벌떡 일어섰다. 조끼 주머니가 달린 토끼도, 거기서 꺼낼 시계를 가진 토끼도 여태껏 본 적이 없다는 사실이 번뜩 떠올랐던 것이다. 호기심에 불이 붙은 앨리스는 들판을 가로질러 토끼를 쫓았고, 다행히도 토끼가 울타리 아래의 커다란 토끼 굴로 쏙 들어가는 모습을 아슬아슬하게 볼 수 있었다.",
    "다음 순간 앨리스도 토끼를 따라 뛰어들었다. 대체 어떻게 다시 나올지는 한 번도 생각하지 않았다.",
    "토끼 굴은 얼마 동안 터널처럼 곧게 이어지다가 갑자기 아래로 푹 꺾였다. 너무도 갑작스러워서 멈출 생각조차 하기 전에 앨리스는 아주 깊은 우물 아래로 떨어지고 있었다.",
    "우물이 몹시 깊었거나, 아니면 앨리스가 아주 천천히 떨어졌던 모양이다. 내려가는 동안 주위를 둘러보고 다음에는 무슨 일이 벌어질지 궁금해할 시간이 넉넉했다. 먼저 아래를 내려다보며 어디에 닿게 될지 알아보려 했지만, 너무 어두워 아무것도 보이지 않았다. 그다음 우물 벽을 보니 찬장과 책꽂이가 빼곡했고, 여기저기 못에 지도와 그림이 걸려 있었다. 지나가며 선반에서 단지 하나를 집어 들었다. ‘오렌지 마멀레이드’라는 딱지가 붙어 있었지만, 무척 실망스럽게도 텅 비어 있었다. 밑에 있는 누군가를 죽일까 봐 단지를 떨어뜨릴 수는 없어서, 떨어지며 스쳐 가는 찬장 하나에 가까스로 도로 집어넣었다.",
    "‘이런!’ 앨리스는 생각했다. ‘이렇게 떨어지고 나면 계단에서 굴러 떨어지는 것쯤은 아무렇지도 않겠어! 집에 있는 사람들이 나를 얼마나 용감하다고 할까! 지붕 꼭대기에서 떨어져도 입도 뻥긋하지 않을걸!’ 아마 정말 그랬을 것이다.",
    "아래로, 아래로, 또 아래로. 이 추락은 영영 끝나지 않는 걸까? “지금까지 몇 마일이나 떨어졌을까?” 앨리스는 소리 내어 말했다. “지구 중심 가까이 왔겠어. 어디 보자, 아마 4천 마일쯤 내려왔을 거야—” 보다시피 앨리스는 학교 수업에서 이런 것을 몇 가지 배웠다. 들어 줄 사람도 없으니 지식을 뽐내기에는 그리 좋은 기회가 아니었지만, 입으로 되뇌는 연습은 되었다. “—그래, 대략 그만한 거리야. 그런데 지금 내 위도나 경도는 얼마일까?” 앨리스는 위도가 무엇인지도 경도가 무엇인지도 전혀 몰랐지만, 입에 올리면 제법 근사하고 거창하게 들리는 말이라고 생각했다.",
    "잠시 뒤 앨리스가 다시 입을 열었다. “지구를 완전히 뚫고 떨어지면 어쩌지! 머리를 아래로 하고 걷는 사람들 사이로 불쑥 나오면 정말 우습겠다! 이름이 아마 대척점 사람들이었지—” 이번에는 아무도 듣고 있지 않은 게 앨리스로서도 꽤 다행이었다. 아무래도 맞는 말 같지 않았기 때문이다. “—하지만 그곳이 어느 나라인지는 물어봐야겠지. ‘실례합니다, 부인. 여기가 뉴질랜드인가요, 오스트레일리아인가요?’” 앨리스는 말하면서 절을 해 보려 했다. 공중에서 떨어지는 중에 절을 한다니! 여러분이라면 해낼 수 있을까? “‘어쩜 저렇게 무식한 꼬마가 다 있담!’ 하고 생각할 거야. 안 돼, 물어보면 안 되겠다. 어딘가에 쓰여 있을지도 몰라.”",
    "아래로, 아래로, 또 아래로. 달리 할 일도 없어서 앨리스는 이내 다시 이야기를 시작했다. “다이너가 오늘 밤 나를 몹시 보고 싶어 하겠지!” 다이너는 고양이였다. “차 마실 때 다이너에게 우유 한 접시 주는 걸 잊지 말아야 할 텐데. 사랑하는 다이너! 네가 나와 함께 여기 내려와 있으면 좋을 텐데! 공중에는 생쥐가 없겠지만 박쥐는 잡을 수 있을 거야. 박쥐는 생쥐랑 아주 비슷하잖아. 그런데 고양이가 박쥐를 먹나?” 이쯤에서 앨리스는 조금 졸려져 몽롱한 목소리로 “고양이가 박쥐를 먹나? 고양이가 박쥐를 먹나?” 하고 계속 중얼거렸다. 때로는 “박쥐가 고양이를 먹나?”라고도 했다. 어느 쪽 질문에도 답할 수 없으니 순서를 어떻게 놓든 별 상관이 없었다. 깜빡 잠이 들려던 앨리스는 다이너와 손을 잡고 걸으며 아주 진지하게 “자, 다이너, 사실대로 말해 봐. 너 박쥐를 먹어 본 적 있니?” 하고 묻는 꿈을 막 꾸기 시작했다. 그 순간 쿵! 쿵! 나뭇가지와 마른 잎 더미 위로 떨어지며 추락이 끝났다."
  ]
};

const EXPERIENCES = [
  { id: "paragraph", name: "현재 문단 방식", mode: "tap", tutorial: "댓글을 보고 싶은 문단을 탭하세요. 같은 문단에 새 댓글도 남길 수 있어요." },
  { id: "sentence-tap", name: "여러 문장—탭", mode: "tap", tutorial: "시작 문장과 마지막 문장을 차례로 탭한 뒤, ‘댓글 달기’를 누르세요." },
  { id: "sentence-drag", name: "여러 문장—드래그", mode: "drag", tutorial: "본문을 길게 누르고 선택 핸들을 움직이세요. 선택한 문장 전체에 댓글을 달 수 있어요." },
  { id: "sentence", name: "한 문장", mode: "tap", tutorial: "댓글을 보고 남기고 싶은 문장을 한 번 탭하세요." },
  { id: "words", name: "여러 단어", mode: "drag", tutorial: "본문을 길게 누르고 선택 핸들을 움직여 원하는 단어 범위를 고르세요." }
];

const BASE_COMMENTS = [
  { id: "c1", author: "민서", passage: 0, focusSentence: 1, multiStart: 0, multiEnd: 1, quote: "그림도 없고 대화도 없었다", text: "앨리스가 책의 재미를 판단하는 기준이 솔직해서 첫 문단부터 성격이 선명하게 느껴져요." },
  { id: "c2", author: "준호", passage: 0, focusSentence: 2, multiStart: 1, multiEnd: 2, quote: "대화도 없었다. ‘그림도 대화도 없는 책", text: "어른의 독서와 아이의 독서를 가르는 질문 같아요. 우리 모임 책은 앨리스에게 합격일까요?" },
  { id: "c3", author: "서연", passage: 1, focusSentence: 2, multiStart: 1, multiEnd: 2, quote: "분홍빛 눈을 가진 흰 토끼", text: "지루함이 최고조에 이른 순간 토끼가 나타나는 리듬이 좋아요. 모험은 늘 빈틈으로 들어오는 것 같네요." },
  { id: "c4", author: "지우", passage: 2, focusSentence: 3, multiStart: 2, multiEnd: 3, quote: "모두 아주 자연스러워 보였다", text: "이상한 일을 이상하게 여기지 않는 태도가 이상한 나라로 가는 첫 번째 자격처럼 보여요." },
  { id: "c5", author: "도윤", passage: 2, focusSentence: 3, multiStart: 3, multiEnd: 4, quote: "마땅히 놀랐어야 했다는 생각", text: "나중에야 놀랐어야 했다고 깨닫는 대목이 재밌어요. 독자만 먼저 현실의 규칙을 떠올리게 하네요." },
  { id: "c6", author: "하린", passage: 5, focusSentence: 3, multiStart: 1, multiEnd: 3, quote: "찬장과 책꽂이가 빼곡했고", text: "추락하는 공간에 생활의 물건들이 붙어 있으니 무섭기보다 꿈속을 구경하는 기분이 들어요." },
  { id: "c7", author: "유진", passage: 7, focusSentence: 0, multiStart: 0, multiEnd: 1, quote: "아래로, 아래로, 또 아래로", text: "반복되는 문장이 실제로 오래 떨어지는 감각을 만들어요. 소리 내어 읽으면 더 잘 느껴집니다." },
  { id: "c8", author: "현우", passage: 9, focusSentence: 9, multiStart: 8, multiEnd: 10, quote: "고양이가 박쥐를 먹나?", text: "논리가 잠에 녹아 문장 순서까지 뒤섞이는 장면이 귀여워요. 말장난이 번역에서도 살아 있네요." }
];

const STORAGE_KEY = "yeobaek-comment-unit-prototype-v1";
const app = document.getElementById("app");
const liveRegion = document.getElementById("live-region");
let state = loadState();
let activeDialog = null;
let returnFocus = null;
let selectionTarget = null;
let tapRange = null;
let readerCleanup = null;
let selectionFrame = 0;
let normalizingSelection = false;
let suppressAnnotationClickUntil = 0;
let noticeTimer = 0;

function defaultState() {
  return {
    screen: "start",
    step: 0,
    seenTutorials: [],
    userComments: Object.fromEntries(EXPERIENCES.map(({ id }) => [id, []]))
  };
}

function loadState() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(STORAGE_KEY));
    if (!saved || !saved.userComments) return defaultState();
    return { ...defaultState(), ...saved };
  } catch (_) {
    return defaultState();
  }
}

function saveState() {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch (_) {
    // Storage is an enhancement; the in-memory experience remains usable.
  }
}

function escapeHtml(value) {
  return String(value).replace(/[&<>'"]/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[char]));
}

function announce(message) {
  liveRegion.textContent = "";
  requestAnimationFrame(() => { liveRegion.textContent = message; });
}

function splitSentences(text) {
  const results = [];
  let start = 0;
  for (let index = 0; index < text.length; index += 1) {
    if (!".!?。！？".includes(text[index])) continue;
    let end = index + 1;
    while (end < text.length && "!?。！？.’”\"'".includes(text[end])) end += 1;
    if (end < text.length && !/\s/.test(text[end])) continue;
    results.push({ start, end, text: text.slice(start, end) });
    while (end < text.length && /\s/.test(text[end])) end += 1;
    start = end;
    index = end - 1;
  }
  if (start < text.length) results.push({ start, end: text.length, text: text.slice(start) });
  return results.length ? results : [{ start: 0, end: text.length, text }];
}

const SENTENCES = BOOK.passages.map(splitSentences);

function exactRange(comment) {
  const text = BOOK.passages[comment.passage];
  const start = text.indexOf(comment.quote);
  return { passage: comment.passage, start: start >= 0 ? start : SENTENCES[comment.passage][comment.focusSentence].start, end: start >= 0 ? start + comment.quote.length : SENTENCES[comment.passage][comment.focusSentence].end };
}

function rangeForComment(comment, experienceId) {
  const sentences = SENTENCES[comment.passage];
  if (experienceId === "paragraph") return { passage: comment.passage, start: 0, end: BOOK.passages[comment.passage].length };
  if (experienceId === "sentence-tap" || experienceId === "sentence-drag") {
    return { passage: comment.passage, start: sentences[comment.multiStart].start, end: sentences[Math.min(comment.multiEnd, sentences.length - 1)].end };
  }
  if (experienceId === "sentence") {
    const sentence = sentences[Math.min(comment.focusSentence, sentences.length - 1)];
    return { passage: comment.passage, start: sentence.start, end: sentence.end };
  }
  return exactRange(comment);
}

function currentExperience() { return EXPERIENCES[state.step]; }

function allAnnotations() {
  const experience = currentExperience();
  const existing = BASE_COMMENTS.map(comment => ({ ...comment, owner: false, range: rangeForComment(comment, experience.id) }));
  const authored = (state.userComments[experience.id] || []).map(comment => ({ ...comment, owner: true, range: comment.range }));
  return [...existing, ...authored];
}

function render() {
  readerCleanup?.();
  readerCleanup = null;
  closeDialog(false);
  clearSelectionState();
  if (state.screen === "start") return renderStart();
  if (state.screen === "complete") return renderComplete();
  renderReader();
}

function renderStart() {
  app.innerHTML = `<section class="welcome">
    <p class="brand" aria-label="여백">여백</p>
    <h2>댓글 단위 체험</h2>
    <p>같은 글을 다섯 가지 방식으로 읽으며 댓글을 보고 남겨보세요.</p>
    <button class="primary-button" id="start-button">체험 시작</button>
  </section>`;
  document.getElementById("start-button").addEventListener("click", () => {
    state.screen = "reader";
    state.step = 0;
    saveState();
    render();
    maybeShowTutorial();
  });
}

function renderComplete() {
  app.innerHTML = `<section class="complete">
    <p class="brand" aria-hidden="true">여백</p>
    <h2>모든 체험을 마쳤어요</h2>
    <p>진행자와 함께 다섯 가지 방식을 천천히 비교해 보세요.</p>
    <button class="primary-button" id="restart-button">처음부터 다시 체험</button>
  </section>`;
  document.getElementById("restart-button").addEventListener("click", () => {
    state = defaultState();
    try { sessionStorage.removeItem(STORAGE_KEY); } catch (_) { /* Continue with fresh in-memory state. */ }
    render();
  });
}

function renderReader() {
  const experience = currentExperience();
  app.innerHTML = `<section class="reader-screen">
    <header class="reader-header">
      <div class="experience-row">
        <p class="experience-name">${escapeHtml(experience.name)}</p>
        <span class="step" aria-label="전체 5단계 중 ${state.step + 1}단계">${state.step + 1} / 5</span>
      </div>
      <div class="progress" aria-hidden="true"><span style="width:${(state.step + 1) * 20}%"></span></div>
    </header>
    <div class="book-heading">
      <p class="eyebrow">${escapeHtml(BOOK.chapter)}</p>
      <h1>${escapeHtml(BOOK.title)}</h1>
      <p class="author">${escapeHtml(BOOK.author)}</p>
      <hr class="chapter-divider">
    </div>
    <p id="selection-notice" class="selection-notice" role="status" hidden></p>
    <article id="reader-content" class="reader-content mode-${experience.mode}" aria-label="책 본문"></article>
    <nav class="reader-nav" aria-label="체험 이동">
      <button class="secondary-button" id="previous-button" ${state.step === 0 ? "disabled" : ""}>이전</button>
      <button class="primary-button" id="next-button">${state.step === EXPERIENCES.length - 1 ? "완료" : "다음"}</button>
    </nav>
  </section>`;
  renderPassages();
  bindReaderEvents();
  document.getElementById("previous-button").addEventListener("click", () => moveStep(-1));
  document.getElementById("next-button").addEventListener("click", () => {
    if (state.step === EXPERIENCES.length - 1) {
      state.screen = "complete";
      saveState();
      render();
    } else moveStep(1);
  });
}

function moveStep(delta) {
  state.step = Math.max(0, Math.min(EXPERIENCES.length - 1, state.step + delta));
  saveState();
  render();
  window.scrollTo(0, 0);
  maybeShowTutorial();
}

function maybeShowTutorial() {
  const experience = currentExperience();
  if (state.seenTutorials.includes(experience.id)) return;
  state.seenTutorials.push(experience.id);
  saveState();
  openCenteredDialog(`<div class="tutorial-card" role="dialog" aria-modal="true" aria-labelledby="tutorial-title">
    <span class="count">${state.step + 1} / 5</span>
    <h2 id="tutorial-title">${escapeHtml(experience.name)}</h2>
    <p>${escapeHtml(experience.tutorial)}</p>
    <button class="primary-button" data-dialog-close>알겠어요</button>
  </div>`, "[data-dialog-close]");
}

function renderPassages() {
  const container = document.getElementById("reader-content");
  const annotations = allAnnotations();
  const experience = currentExperience();
  container.innerHTML = BOOK.passages.map((text, passageIndex) => {
    const sentences = SENTENCES[passageIndex];
    const sentenceHtml = sentences.map((sentence, sentenceIndex) => renderSentence(text, passageIndex, sentenceIndex, sentence, annotations)).join("");
    const interactive = experience.id === "paragraph" ? ` role="button" tabindex="0" aria-label="문단 댓글 보기 및 남기기"` : "";
    return `<p class="passage" data-passage="${passageIndex}"${interactive}>${sentenceHtml}</p>`;
  }).join("");
}

function renderSentence(text, passageIndex, sentenceIndex, sentence, annotations) {
  const relevant = annotations.filter(item => item.range.passage === passageIndex && item.range.start < sentence.end && item.range.end > sentence.start);
  const boundaries = new Set([sentence.start, sentence.end]);
  relevant.forEach(item => {
    boundaries.add(Math.max(sentence.start, item.range.start));
    boundaries.add(Math.min(sentence.end, item.range.end));
  });
  const points = [...boundaries].sort((a, b) => a - b);
  let inner = "";
  for (let i = 0; i < points.length - 1; i += 1) {
    const start = points[i];
    const end = points[i + 1];
    if (start === end) continue;
    const ids = relevant.filter(item => item.range.start <= start && item.range.end >= end).map(item => item.id);
    const content = escapeHtml(text.slice(start, end));
    const dragTarget = currentExperience().mode === "drag" ? ` tabindex="0" role="button" aria-label="${ids.length}개의 댓글이 있는 글"` : "";
    inner += ids.length ? `<span class="annotation${ids.length > 1 ? " overlap" : ""}" data-comment-ids="${ids.join(",")}" data-offset="${start}"${dragTarget}>${content}</span>` : content;
  }
  const sentenceInteractive = ["sentence-tap", "sentence"].includes(currentExperience().id) ? ` role="button" tabindex="0" aria-label="문장 댓글 보기 및 남기기"` : "";
  return `<span class="sentence" data-passage="${passageIndex}" data-sentence="${sentenceIndex}" data-start="${sentence.start}" data-end="${sentence.end}"${sentenceInteractive}>${inner}</span>${sentenceIndex < SENTENCES[passageIndex].length - 1 ? " " : ""}`;
}

function bindReaderEvents() {
  const reader = document.getElementById("reader-content");
  const experience = currentExperience();
  const onClick = event => handleReaderActivation(event);
  const onKeydown = event => {
    if (!(["Enter", " "].includes(event.key))) return;
    const target = event.target.closest(".passage, .sentence, .annotation");
    if (!target || !reader.contains(target)) return;
    if (experience.mode === "drag" && !target.matches(".annotation")) return;
    event.preventDefault();
    handleReaderActivation(event, target);
  };
  reader.addEventListener("click", onClick);
  reader.addEventListener("keydown", onKeydown);
  if (experience.mode === "drag") {
    const onSelectionEnd = () => scheduleSelectionRead();
    const onViewportChange = () => scheduleSelectionRead();
    const onSelectionChange = () => {
      if (normalizingSelection) return;
      const selection = window.getSelection();
      if (selection && !selection.isCollapsed) suppressAnnotationClickUntil = Date.now() + 450;
      scheduleSelectionRead();
    };
    reader.addEventListener("mouseup", onSelectionEnd);
    reader.addEventListener("touchend", onSelectionEnd, { passive: true });
    document.addEventListener("selectionchange", onSelectionChange);
    window.addEventListener("scroll", onViewportChange, { passive: true });
    window.visualViewport?.addEventListener("scroll", onViewportChange, { passive: true });
    window.visualViewport?.addEventListener("resize", onViewportChange, { passive: true });
    readerCleanup = () => {
      reader.removeEventListener("click", onClick);
      reader.removeEventListener("keydown", onKeydown);
      reader.removeEventListener("mouseup", onSelectionEnd);
      reader.removeEventListener("touchend", onSelectionEnd);
      document.removeEventListener("selectionchange", onSelectionChange);
      window.removeEventListener("scroll", onViewportChange);
      window.visualViewport?.removeEventListener("scroll", onViewportChange);
      window.visualViewport?.removeEventListener("resize", onViewportChange);
      cancelAnimationFrame(selectionFrame);
    };
  } else {
    readerCleanup = () => {
      reader.removeEventListener("click", onClick);
      reader.removeEventListener("keydown", onKeydown);
    };
  }
}

function handleReaderActivation(event, forcedOrigin = null) {
  const path = event.composedPath?.() || [];
  const origin = forcedOrigin || path.find(node => node instanceof Element && node.matches(".annotation")) || event.target;
  const sentence = origin.closest(".sentence");
  const passage = origin.closest(".passage");
  if (!passage) return;
  const annotation = origin.closest(".annotation");
  const ids = annotation
    ? annotation.dataset.commentIds.split(",")
    : sentence
      ? [...new Set([...sentence.querySelectorAll(".annotation")].flatMap(node => node.dataset.commentIds.split(",")))]
      : [];
  const experience = currentExperience();
  if (experience.mode === "drag") {
    const selection = window.getSelection();
    if (!annotation || Date.now() < suppressAnnotationClickUntil || (selection && !selection.isCollapsed)) return;
    const target = rangeForAnnotation(ids[0]);
    if (shouldChooseRange(ids)) openRangeChooser(ids, target);
    else openCommentSheet(target, ids);
    return;
  }
  if (experience.id === "paragraph") {
    const target = { passage: Number(passage.dataset.passage), start: 0, end: BOOK.passages[passage.dataset.passage].length };
    openCommentSheet(target);
    return;
  }
  if (!sentence) return;
  if (experience.id === "sentence") {
    const target = rangeFromSentence(sentence);
    openCommentSheet(target);
    return;
  }
  if (experience.id === "sentence-tap") {
    if (tapRange && !tapRange.complete) {
      selectTappedSentence(sentence);
    } else if (ids.length) {
      openTapIntentChoice(ids, sentence);
    } else {
      selectTappedSentence(sentence);
    }
  }
}

function openTapIntentChoice(ids, sentence) {
  openSheet(`<div class="sheet short" role="dialog" aria-modal="true" aria-labelledby="choice-title">
    <div class="sheet-handle"></div>
    <div class="sheet-header"><h2 id="choice-title">무엇을 할까요?</h2><button class="icon-button" data-sheet-close aria-label="닫기">×</button></div>
    <div class="sheet-body choice-actions">
      <button class="secondary-button" id="view-comment-choice">댓글 보기</button>
      <button class="primary-button" id="start-range-choice">범위 선택 시작</button>
    </div>
  </div>`, () => {
    document.getElementById("view-comment-choice").addEventListener("click", () => {
      closeDialog();
      if (shouldChooseRange(ids)) openRangeChooser(ids, rangeFromSentence(sentence));
      else openCommentSheet(rangeForAnnotation(ids[0]), ids);
    });
    document.getElementById("start-range-choice").addEventListener("click", () => {
      closeDialog();
      tapRange = null;
      selectTappedSentence(sentence);
    });
  });
}

function selectTappedSentence(sentence) {
  const target = rangeFromSentence(sentence);
  if (tapRange && !tapRange.complete && tapRange.passage !== target.passage) {
    showSelectionNotice("댓글 범위는 같은 문단 안에서만 선택할 수 있어요.");
    return;
  }
  if (!tapRange || tapRange.complete) {
    tapRange = { ...target, anchorSentence: Number(sentence.dataset.sentence), complete: false };
    document.querySelectorAll(".selected-range").forEach(node => node.classList.remove("selected-range"));
    sentence.classList.add("selected-range");
    announce("시작 문장을 선택했습니다. 마지막 문장을 선택하세요.");
    return;
  }
  const endSentence = Number(sentence.dataset.sentence);
  const first = Math.min(tapRange.anchorSentence, endSentence);
  const last = Math.max(tapRange.anchorSentence, endSentence);
  const sentences = SENTENCES[target.passage];
  tapRange = { passage: target.passage, start: sentences[first].start, end: sentences[last].end, complete: true };
  document.querySelectorAll(".selected-range").forEach(node => node.classList.remove("selected-range"));
  for (let index = first; index <= last; index += 1) {
    document.querySelector(`.sentence[data-passage="${target.passage}"][data-sentence="${index}"]`).classList.add("selected-range");
  }
  showContextAction(sentence.getBoundingClientRect(), tapRange);
  announce("문장 범위를 선택했습니다. 댓글 달기 버튼을 누르세요.");
}

function scheduleSelectionRead() {
  cancelAnimationFrame(selectionFrame);
  selectionFrame = requestAnimationFrame(readNativeSelection);
}

function readNativeSelection() {
  if (activeDialog) return;
  const selection = window.getSelection();
  if (!selection || selection.isCollapsed || selection.rangeCount === 0) {
    hideContextAction();
    selectionTarget = null;
    return;
  }
  const range = selection.getRangeAt(0);
  const startPassage = closestPassage(range.startContainer);
  const endPassage = closestPassage(range.endContainer);
  if (!startPassage || !endPassage) {
    hideContextAction();
    selectionTarget = null;
    return;
  }
  if (startPassage !== endPassage) {
    hideContextAction();
    selectionTarget = null;
    showSelectionNotice("댓글 범위는 같은 문단 안에서만 선택할 수 있어요.");
    return;
  }
  const passageIndex = Number(startPassage.dataset.passage);
  const start = offsetWithin(startPassage, range.startContainer, range.startOffset);
  const end = offsetWithin(startPassage, range.endContainer, range.endOffset);
  if (start === end) return;
  selectionTarget = { passage: passageIndex, start: Math.min(start, end), end: Math.max(start, end) };
  if (currentExperience().id === "sentence-drag") {
    const touched = SENTENCES[passageIndex].filter(sentence => sentence.start < selectionTarget.end && sentence.end > selectionTarget.start);
    if (touched.length) {
      selectionTarget.start = touched[0].start;
      selectionTarget.end = touched[touched.length - 1].end;
      setDomSelection(startPassage, selectionTarget.start, selectionTarget.end);
    }
  }
  const activeRange = window.getSelection()?.rangeCount ? window.getSelection().getRangeAt(0) : range;
  const rect = activeRange.getBoundingClientRect();
  showContextAction(rect, selectionTarget);
}

function setDomSelection(passage, start, end) {
  const startPoint = textPointAt(passage, start);
  const endPoint = textPointAt(passage, end);
  if (!startPoint || !endPoint) return;
  const range = document.createRange();
  range.setStart(startPoint.node, startPoint.offset);
  range.setEnd(endPoint.node, endPoint.offset);
  const selection = window.getSelection();
  normalizingSelection = true;
  selection.removeAllRanges();
  selection.addRange(range);
  requestAnimationFrame(() => { normalizingSelection = false; });
}

function textPointAt(root, wantedOffset) {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  let consumed = 0;
  let node;
  while ((node = walker.nextNode())) {
    if (consumed + node.data.length >= wantedOffset) return { node, offset: wantedOffset - consumed };
    consumed += node.data.length;
  }
  return null;
}

function showSelectionNotice(message) {
  const notice = document.getElementById("selection-notice");
  if (!notice) return;
  clearTimeout(noticeTimer);
  notice.textContent = message;
  notice.hidden = false;
  announce(message);
  noticeTimer = window.setTimeout(() => { notice.hidden = true; }, 2600);
}

function closestPassage(node) {
  return (node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement)?.closest(".passage") || null;
}

function offsetWithin(root, node, offset) {
  const range = document.createRange();
  range.selectNodeContents(root);
  try { range.setEnd(node, offset); } catch (_) { return 0; }
  return range.toString().length;
}

function showContextAction(rect, target) {
  hideContextAction();
  const button = document.createElement("button");
  button.className = "context-action";
  button.id = "context-action";
  button.textContent = "댓글 달기";
  button.style.left = `${Math.max(72, Math.min(window.innerWidth - 72, rect.left + rect.width / 2))}px`;
  button.style.top = `${Math.max(58, rect.top - 7)}px`;
  button.addEventListener("click", () => {
    window.getSelection()?.removeAllRanges();
    hideContextAction();
    openCommentSheet(target, []);
  });
  document.body.appendChild(button);
}

function hideContextAction() { document.getElementById("context-action")?.remove(); }

function clearSelectionState() {
  hideContextAction();
  window.getSelection()?.removeAllRanges();
  selectionTarget = null;
  tapRange = null;
}

function rangeFromSentence(sentence) {
  return { passage: Number(sentence.dataset.passage), start: Number(sentence.dataset.start), end: Number(sentence.dataset.end) };
}

function rangeForAnnotation(id) {
  return allAnnotations().find(item => item.id === id)?.range || null;
}

function shouldChooseRange(ids) {
  if (!["sentence-tap", "sentence-drag", "words"].includes(currentExperience().id)) return false;
  const uniqueRanges = new Set(ids.map(id => rangeForAnnotation(id)).filter(Boolean).map(range => `${range.passage}:${range.start}:${range.end}`));
  return uniqueRanges.size > 1;
}

function openRangeChooser(ids, fallbackTarget) {
  const items = ids.map(id => allAnnotations().find(item => item.id === id)).filter(Boolean);
  openSheet(`<div class="sheet short" role="dialog" aria-modal="true" aria-labelledby="range-title">
    <div class="sheet-handle"></div>
    <div class="sheet-header"><h2 id="range-title">댓글 범위를 선택하세요</h2><button class="icon-button" data-sheet-close aria-label="닫기">×</button></div>
    <div class="sheet-body"><div class="range-list">
      ${items.map(item => `<button class="range-option" data-range-id="${item.id}"><strong>${escapeHtml(item.author)}의 댓글</strong><span>“${escapeHtml(textForRange(item.range))}”</span></button>`).join("")}
    </div></div>
  </div>`, () => {
    document.querySelectorAll("[data-range-id]").forEach(button => button.addEventListener("click", () => {
      const id = button.dataset.rangeId;
      const item = items.find(candidate => candidate.id === id);
      closeDialog();
      openCommentSheet(item?.range || fallbackTarget, [id]);
    }));
  });
}

function commentsForTarget(target, preferredIds) {
  const all = allAnnotations();
  if (preferredIds?.length) {
    return all.filter(item => preferredIds.includes(item.id) || (item.owner && item.range.passage === target.passage && item.range.start === target.start && item.range.end === target.end));
  }
  return all.filter(item => item.range.passage === target.passage && item.range.start === target.start && item.range.end === target.end);
}

function openCommentSheet(target, preferredIds = []) {
  if (!target) return;
  const comments = commentsForTarget(target, preferredIds);
  const quote = textForRange(target);
  openSheet(`<div class="sheet" role="dialog" aria-modal="true" aria-labelledby="comments-title">
    <div class="sheet-handle"></div>
    <div class="sheet-header"><h2 id="comments-title">댓글</h2><button class="icon-button" data-sheet-close aria-label="닫기">×</button></div>
    <div class="sheet-body">
      <blockquote class="quote">${escapeHtml(quote)}</blockquote>
      <div class="comments" id="comment-list">
        ${comments.length ? comments.map(renderCommentCard).join("") : `<p class="empty">아직 이 대목에 댓글이 없어요.</p>`}
      </div>
      <form class="comment-form" id="comment-form" novalidate>
        <label for="comment-input">댓글 남기기</label>
        <textarea id="comment-input" maxlength="1000" placeholder="이 대목에서 떠오른 생각을 남겨보세요"></textarea>
        <p class="form-error" id="form-error" role="alert"></p>
        <div class="form-row"><span class="counter" id="comment-counter">0 / 1000</span><button class="primary-button" type="submit" disabled>등록</button></div>
      </form>
    </div>
  </div>`, () => bindCommentSheet(target, preferredIds));
}

function renderCommentCard(comment) {
  return `<article class="comment-card">
    <div class="comment-meta"><strong>${escapeHtml(comment.author)}${comment.owner ? " · 나" : ""}</strong><span>${comment.owner ? "방금 전" : "함께 읽는 중"}</span></div>
    <p class="comment-text">${escapeHtml(comment.text)}</p>
    ${comment.owner ? `<div class="comment-actions"><button class="text-button" data-edit-comment="${comment.id}">수정</button><button class="text-button danger" data-delete-comment="${comment.id}">삭제</button></div>` : ""}
  </article>`;
}

function bindCommentSheet(target, preferredIds) {
  const form = document.getElementById("comment-form");
  const input = document.getElementById("comment-input");
  const submit = form.querySelector("button[type=submit]");
  const counter = document.getElementById("comment-counter");
  const error = document.getElementById("form-error");
  input.addEventListener("input", () => {
    counter.textContent = `${input.value.length} / 1000`;
    submit.disabled = !input.value.trim() || input.value.length > 1000;
    error.textContent = "";
  });
  form.addEventListener("submit", event => {
    event.preventDefault();
    const text = input.value.trim();
    if (!text) { error.textContent = "댓글 내용을 입력해 주세요."; return; }
    const experienceId = currentExperience().id;
    state.userComments[experienceId].push({ id: `u-${Date.now()}-${Math.random().toString(16).slice(2)}`, author: "나", text, range: target });
    saveState();
    closeDialog();
    renderPassages();
    announce("댓글을 등록했습니다.");
    openCommentSheet(target, preferredIds);
  });
  document.querySelectorAll("[data-edit-comment]").forEach(button => button.addEventListener("click", () => editComment(button.dataset.editComment, target, preferredIds)));
  document.querySelectorAll("[data-delete-comment]").forEach(button => button.addEventListener("click", () => confirmDelete(button.dataset.deleteComment, target, preferredIds)));
}

function editComment(id, target, preferredIds) {
  const comments = state.userComments[currentExperience().id];
  const comment = comments.find(item => item.id === id);
  if (!comment) return;
  const input = document.getElementById("comment-input");
  const form = document.getElementById("comment-form");
  input.value = comment.text;
  input.dispatchEvent(new Event("input"));
  input.focus();
  form.querySelector("label").textContent = "댓글 수정하기";
  form.querySelector("button[type=submit]").textContent = "저장";
  const replacement = form.cloneNode(true);
  form.replaceWith(replacement);
  const nextInput = replacement.querySelector("textarea");
  const nextSubmit = replacement.querySelector("button[type=submit]");
  const nextCounter = replacement.querySelector(".counter");
  nextInput.addEventListener("input", () => { nextCounter.textContent = `${nextInput.value.length} / 1000`; nextSubmit.disabled = !nextInput.value.trim(); });
  replacement.addEventListener("submit", event => {
    event.preventDefault();
    const text = nextInput.value.trim();
    if (!text) return;
    comment.text = text;
    saveState();
    closeDialog();
    announce("댓글을 수정했습니다.");
    openCommentSheet(target, preferredIds);
  });
  nextInput.focus();
}

function confirmDelete(id, target, preferredIds) {
  closeDialog();
  openCenteredDialog(`<div class="confirm-card" role="alertdialog" aria-modal="true" aria-labelledby="delete-title" aria-describedby="delete-description">
    <h2 id="delete-title">댓글을 삭제할까요?</h2>
    <p id="delete-description">삭제한 댓글은 되돌릴 수 없어요.</p>
    <div class="confirm-actions"><button class="secondary-button" data-dialog-close>취소</button><button class="primary-button" id="confirm-delete">삭제</button></div>
  </div>`, "[data-dialog-close]", () => {
    document.getElementById("confirm-delete").addEventListener("click", () => {
      const experienceId = currentExperience().id;
      state.userComments[experienceId] = state.userComments[experienceId].filter(item => item.id !== id);
      saveState();
      closeDialog();
      renderPassages();
      announce("댓글을 삭제했습니다.");
      openCommentSheet(target, preferredIds);
    });
  });
}

function textForRange(range) {
  const text = BOOK.passages[range.passage].slice(range.start, range.end).trim();
  return text.length > 180 ? `${text.slice(0, 177)}…` : text;
}

function openSheet(markup, onOpen) {
  returnFocus = document.activeElement;
  setModalState(true);
  document.body.insertAdjacentHTML("beforeend", `<div class="scrim" data-sheet-close></div>${markup}`);
  activeDialog = document.querySelector(".sheet");
  bindDialogBasics("[data-sheet-close]");
  onOpen?.();
  window.setTimeout(() => activeDialog?.querySelector("button, textarea")?.focus(), 0);
}

function openCenteredDialog(markup, initialSelector, onOpen) {
  returnFocus = document.activeElement;
  setModalState(true);
  document.body.insertAdjacentHTML("beforeend", `<div class="scrim"></div>${markup}`);
  activeDialog = document.querySelector(".tutorial-card, .confirm-card");
  bindDialogBasics("[data-dialog-close]");
  onOpen?.();
  window.setTimeout(() => activeDialog?.querySelector(initialSelector)?.focus(), 0);
}

function bindDialogBasics(closeSelector) {
  document.querySelectorAll(closeSelector).forEach(element => element.addEventListener("click", () => closeDialog()));
  document.addEventListener("keydown", dialogKeydown);
}

function dialogKeydown(event) {
  if (!activeDialog) return;
  if (event.key === "Escape") { closeDialog(); return; }
  if (event.key !== "Tab") return;
  const focusable = [...activeDialog.querySelectorAll("button:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex='-1'])")];
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
}

function closeDialog(restoreFocus = true) {
  document.querySelectorAll(".scrim, .sheet, .tutorial-card, .confirm-card").forEach(node => node.remove());
  document.removeEventListener("keydown", dialogKeydown);
  activeDialog = null;
  setModalState(false);
  if (restoreFocus) {
    const focusTarget = returnFocus?.isConnected ? returnFocus : document.querySelector(".reader-header, .welcome button, .complete button, main button");
    if (focusTarget) {
      if (!focusTarget.matches("button, textarea, [tabindex]")) focusTarget.setAttribute("tabindex", "-1");
      focusTarget.focus();
    }
  }
  returnFocus = null;
}

function setModalState(open) {
  app.inert = open;
  if (open) app.setAttribute("aria-hidden", "true");
  else app.removeAttribute("aria-hidden");
  document.body.classList.toggle("modal-open", open);
}

render();
if (state.screen === "reader") window.setTimeout(maybeShowTutorial, 0);
