Hi Team,

We are currently facing an issue in the FFF Sanction flow when local chars are present in the payload. Due to the presence of local chars in the fields, we are currently unable to split and map the data correctly to the Enrich fields.

Based on our analysis, **to support local chars in VolPay, changes are required in the existing implementation**.

The proposed changes are as follows:

* For the mentioned fields **59ln and 52ln**, we will split the received data based on the new line (`\n`) character.
* After splitting:

  * The first part will be mapped to `<EnrichNm>`.
  * The second part will be mapped to `<EnrichAdr>`.
* If no new line character is present, the complete received data will be mapped to `<EnrichNm>`.

**Examples:**

**Case 1 – No new line character:**

`<59ln><CHINESE CHARS></59ln>`

→ `<EnrichNm><CHINESE CHARS></EnrichNm>`

**Case 2 – New line character present:**

`<59ln><CHINESE CHARS>\n<CHINESE CHARS Adr></59ln>`

→ `<EnrichNm><CHINESE CHARS></EnrichNm>`
→ `<EnrichAdr><CHINESE CHARS Adr></EnrichAdr>`

Please let us know if there are any concerns with the above approach or if we can proceed with these changes.
