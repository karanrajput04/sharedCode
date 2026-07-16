Hi G,

We have completed the development from the VPLE side for RO GF. However, while reviewing the implementation, we identified a gap in the requirement understanding/alignment.

In the current PROD setup for RO, VP and DQE send their respective JSON payloads directly to BigData, and VPLE is not involved in that flow. Additionally, VP's BigData flow is currently not onboarded in EL.

If we intend to continue with the existing PROD approach, we will need to enable the VPLE BigData flow to support the required functionality.

Could you please review this and confirm the expected approach so that we can proceed with the implementation accordingly? Let us know if a discussion is required to align on the requirement.
