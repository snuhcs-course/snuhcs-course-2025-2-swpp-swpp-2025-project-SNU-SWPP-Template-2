from app.features.selfaware.service import (
    QuestionService,
    ValueScoreService,
    ValueMapService
)

def test_generate_single_category_question(mocker):
    question_repo = mocker.stub("QuestionRepository")
    question_repo.create_question = mocker.MagicMock(side_effect=lambda user_id, question_type, text: text)
    journal_repo = mocker.stub("JournalRepository")
    question_service = QuestionService(journal_repository=journal_repo,
                                       question_repository=question_repo)
    result = question_service.generate_single_category_question(1)
    
    assert type(result) == str
    question_repo.create_question.assert_called_once()

def test_generate_multi_category_question(mocker):
    question_repo = mocker.stub("QuestionRepository")
    question_repo.create_question = mocker.MagicMock(side_effect=lambda user_id, question_type, text: text)
    journal_repo = mocker.stub("JournalRepository")
    question_service = QuestionService(journal_repository=journal_repo,
                                       question_repository=question_repo)
    result = question_service.generate_multi_category_question(1)
    
    assert type(result) == str
    question_repo.create_question.assert_called_once()


def test_generate_selfaware_question(mocker):
    class FakeJournal:
        def __init__(self, content):
            self.content: str = content

    question_repo = mocker.stub("QuestionRepository")
    question_repo.create_question = mocker.MagicMock(side_effect=lambda user_id, question_type, text: text)
    journal_repo = mocker.stub("JournalRepository")
    journal_repo.list_journals_by_user = mocker.MagicMock(return_value = [FakeJournal("오늘은 치킨을 먹었다.")])
    question_service = QuestionService(journal_repository=journal_repo,
                                       question_repository=question_repo)
    result = question_service.generate_selfaware_question(1)
    
    assert type(result) == str
    question_repo.create_question.assert_called_once()

def test_extract_value_score_from_answer(mocker):
    class FakeAnswer:
        def __init__(self, text):
            self.text: str = text
    class FakeQuestion:
        def __init__(self, text):
            self.text: str = text
    question_repo = mocker.stub("QuestionRepository")
    question_repo.get_question_by_id = mocker.MagicMock(return_value = FakeQuestion("치킨을 먹으며 느낀 행복은 무엇 때문이었나요?"))
    answer_repo = mocker.stub("AnswerRepository")
    answer_repo.get_answer_by_id = mocker.MagicMock(return_value = FakeAnswer("내 돈으로 벌어 내가 산 치킨을 먹으며 나 자신이 증명됨을 느꼈기 때문입니다."))
    value_score_repo = mocker.stub("ValueScoreRepository")
    value_score_repo.create_value_score = mocker.MagicMock(return_value = True)
    value_map_repo = mocker.stub("ValueMapRepository")
    value_map_repo.get_by_user = mocker.MagicMock(return_value = True)
    value_map_repo.update_by_value_score = mocker.MagicMock(return_value = True)
    value_score_service = ValueScoreService(question_repository=question_repo,
                                            answer_repository=answer_repo,
                                            value_score_repository=value_score_repo,
                                            value_map_repository=value_map_repo)
    
    result = value_score_service.extract_value_score_from_answer(1,1,1)

    for result_element in result:
        assert result_element.category_key in ["Neuroticism", "Extraversion", "Openness to Experience", "Agreeableness", "Conscientiousness"]
        assert type(result_element.value) == str
        assert 0 <= result_element.confidence <= 1
        assert 0 <= result_element.intensity <= 1
        assert result_element.polarity in [-1, 0, 1]
        assert type(result_element.evidence) == str

def test_get_top_value_scores(mocker):
    class FakeValueScore:
        def __init__(self, polarity, value, intensity, category):
            self.polarity = polarity
            self.value = value
            self.intensity = intensity
            self.category = category
    question_repo = mocker.stub("QuestionRepository")
    answer_repo = mocker.stub("AnswerRepository")
    value_score_repo = mocker.stub("ValueScoreRepository")
    value_score_repo.get_top_5_value_scores = mocker.MagicMock(return_value = [FakeValueScore(1,"친절",90,"Neuroticism"), FakeValueScore(1,"사교성",90,"Extraversion"), FakeValueScore(-1,"이기성",85,"Openness to Experience"), FakeValueScore(1,"진취성",80,"Agreeableness"), FakeValueScore(0,"공상",75,"성실성")])
    value_map_repo = mocker.stub("ValueMapRepository")
    value_score_service = ValueScoreService(question_repository=question_repo,
                                            answer_repository=answer_repo,
                                            value_score_repository=value_score_repo,
                                            value_map_repository=value_map_repo)
    
    result = value_score_service.get_top_value_scores(1)

    for result_element in result:
        assert type(result_element["value"]) == str
        assert type(result_element["intensity"]) == int
    assert len(result) == 4

def test_generate_comment(mocker):
    class FakeValuemap:
        def __init__(self,score_0,score_1,score_2,score_3,score_4):
            self.score_0 = score_0
            self.score_1 = score_1
            self.score_2 = score_2
            self.score_3 = score_3
            self.score_4 = score_4
    value_score_repo = mocker.stub("ValueScoreRepository")
    value_map_repo = mocker.stub("ValueMapRepository")
    value_map_repo.get_by_user = mocker.MagicMock(return_value = FakeValuemap(11.1, 22.2, 33.3, 44.4, 55.5))
    value_map_repo.generate_comment = mocker.MagicMock(side_effect=lambda user_id, personality_insight, comment: (personality_insight, comment))
    answer_repo = mocker.stub("AnswerRepository")
    value_map_service = ValueMapService(value_map_repository=value_map_repo,
                                        value_score_repository=value_score_repo,
                                        answer_repository=answer_repo)

    result = value_map_service.generate_comment(1)
    assert type(result[0]) == str # type: ignore
    assert type(result[1]) == str # type: ignore
