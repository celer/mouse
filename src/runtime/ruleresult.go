package mouse;

type RuleResult interface {
	LHS() *Phrase;
	RHSSize() int;
	RHS(i int) *Phrase;
	RHSText(i int, j int) string;
}

